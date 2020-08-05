package com.letsconfig.client.repository.grpc

import com.letsconfig.client.ChangingObservable
import com.letsconfig.client.DynamicValue
import com.letsconfig.client.PropertyItem
import com.letsconfig.client.Subscriber
import com.letsconfig.client.metrics.LibraryMetrics
import com.letsconfig.client.metrics.Metrics
import com.letsconfig.client.repository.ConfigurationRepository
import com.letsconfig.sdk.extension.createLogger
import com.letsconfig.sdk.proto.ConfigurationServiceGrpc
import com.letsconfig.sdk.proto.SubscribeApplicationRequest
import com.letsconfig.sdk.proto.UpdateReceived
import com.letsconfig.sdk.proto.WatchRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

private const val INITIAL_TIMEOUT_SEC: Long = 10L
private const val UNKNOWN_VERSION: Long = -1

class GrpcConfigurationRepository(
        private val applicationHostname: String,
        private val defaultApplicationName: String,
        private val serverHostname: String,
        private val serverPort: Int,
        private val libraryMetrics: LibraryMetrics
) : ConfigurationRepository {

    private val log = createLogger()
    private lateinit var asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub
    private lateinit var channel: ManagedChannel
    private lateinit var subscribeObserver: StreamObserver<WatchRequest>
    private lateinit var watchObserver: WatchObserver
    private val appWatchMappers = ConcurrentHashMap<String, WatchState>()

    @Synchronized
    override fun start() {
        watchObserver = WatchObserver(
                onUpdate = { appName, updates, lastVersion ->
                    val watchState: WatchState = appWatchMappers[appName]!!
                    if (lastVersion <= watchState.lastVersion) {
                        if (updates.isNotEmpty()) {
                            log.debug("Obsolete value has come, known version = ${watchState.lastVersion}," +
                                    "received = ${lastVersion}, applicationName = $appName, updateSize = ${updates.size}")
                        }
                        return@WatchObserver
                    }
                    val filteredUpdates = updates.filter { it.version > watchState.lastVersion }
                    if (filteredUpdates.size != updates.size) {
                        log.debug("Some updates where filtered (they are obsolete)" +
                                ", before size = ${updates.size}, after size = ${filteredUpdates.size}")
                        libraryMetrics.increment(Metrics.SKIPPED_OBSOLETE_UPDATES)
                    }
                    watchState.observable.setValue(filteredUpdates)
                    watchState.lastVersion = lastVersion
                    subscribeObserver.onNext(WatchRequest.newBuilder()
                            .setType(WatchRequest.Type.UPDATE_RECEIVED)
                            .setUpdateReceived(UpdateReceived.newBuilder()
                                    .setApplicationName(appName)
                                    .setVersion(lastVersion)
                                    .build())
                            .build())
                },
                onEnd = {
                    thread {
                        log.info("Resubscribe started, waiting for 2 seconds")
                        try {
                            channel.shutdownNow()
                        } catch (e: Exception) {
                            log.warn("Exception during shutdown", e)
                        }
                        Thread.sleep(2000)
                        initialize()
                    }
                })
        initialize()
    }

    @Synchronized
    private fun initialize() {
        channel = ManagedChannelBuilder.forAddress(serverHostname, serverPort)
                .usePlaintext()
                .keepAliveTime(5000, TimeUnit.MILLISECONDS)
                .build()
        asyncClient = ConfigurationServiceGrpc.newStub(channel)

        subscribeObserver = asyncClient
                .watchChanges(watchObserver)
        for (watchState in appWatchMappers) {
            val subscribeRequest = SubscribeApplicationRequest
                    .newBuilder()
                    .setApplicationName(watchState.value.appName)
                    .setHostName(applicationHostname)
                    .setLastKnownVersion(watchState.value.lastVersion)
                    .build()
            subscribeObserver.onNext(WatchRequest.newBuilder()
                    .setType(WatchRequest.Type.SUBSCRIBE_APPLICATION)
                    .setSubscribeApplicationRequest(subscribeRequest)
                    .build())
            log.info("Resubscribed to app = ${watchState.value.appName} and lastVersion = ${watchState.value.lastVersion}")
        }
        log.info("Watch subscription is (re)initialized")
    }

    override fun subscribeToProperties(appName: String): DynamicValue<List<PropertyItem.Updated>, List<PropertyItem>> {
        val currentObservable = ChangingObservable<List<PropertyItem>>()
        synchronized(this) {
            appWatchMappers[appName] = WatchState(appName, UNKNOWN_VERSION, currentObservable)
            val subscribeRequest = SubscribeApplicationRequest
                    .newBuilder()
                    .setApplicationName(appName)
                    .setDefaultApplicationName(defaultApplicationName)
                    .setHostName(applicationHostname)
                    .build()
            subscribeObserver.onNext(WatchRequest.newBuilder()
                    .setType(WatchRequest.Type.SUBSCRIBE_APPLICATION)
                    .setSubscribeApplicationRequest(subscribeRequest)
                    .build())

        }

        val future = CompletableFuture<List<PropertyItem.Updated>>()
        currentObservable.onSubscribe(object : Subscriber<List<PropertyItem>> {
            override fun process(value: List<PropertyItem>) {
                val snapshot = value.filterIsInstance<PropertyItem.Updated>()
                        .map { PropertyItem.Updated(it.applicationName, it.name, it.version, it.value) }
                future.complete(snapshot)
            }
        })
        var initialProperties: List<PropertyItem.Updated>
        while (true) {
            try {
                initialProperties = future.get(INITIAL_TIMEOUT_SEC, TimeUnit.SECONDS)
                break
            } catch (e: TimeoutException) {
                log.warn("Unable to get initial configuration for seconds = $INITIAL_TIMEOUT_SEC seconds, retrying..")
            }
        }
        log.info("Initial properties for app = $appName received with size = ${initialProperties.size}")
        return DynamicValue(initialProperties, currentObservable)
    }

    @Synchronized
    override fun stop() {
        subscribeObserver.onCompleted()
        watchObserver.isStopped = true
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
    }
}

private data class WatchState(val appName: String, var lastVersion: Long, val observable: ChangingObservable<List<PropertyItem>>)