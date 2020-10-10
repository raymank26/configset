package com.configset.client.repository.grpc

import com.configset.client.ChangingObservable
import com.configset.client.DynamicValue
import com.configset.client.PropertyItem
import com.configset.client.Subscriber
import com.configset.client.metrics.LibraryMetrics
import com.configset.client.metrics.Metrics
import com.configset.client.repository.ConfigurationRepository
import com.configset.sdk.extension.createLoggerStatic
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.sdk.proto.SubscribeApplicationRequest
import com.configset.sdk.proto.UpdateReceived
import com.configset.sdk.proto.WatchRequest
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
private val LOG = createLoggerStatic<GrpcConfigurationRepository>()

class GrpcConfigurationRepository(
        private val applicationHostname: String,
        private val defaultApplicationName: String,
        private val serverHostname: String,
        private val serverPort: Int,
        private val libraryMetrics: LibraryMetrics
) : ConfigurationRepository {

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
                            LOG.debug("Obsolete value has come, known version = ${watchState.lastVersion}," +
                                    "received = ${lastVersion}, applicationName = $appName, updateSize = ${updates.size}")
                        }
                        return@WatchObserver
                    }
                    val filteredUpdates = updates.filter { it.version > watchState.lastVersion }
                    if (filteredUpdates.size != updates.size) {
                        LOG.debug("Some updates where filtered (they are obsolete)" +
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
                        LOG.info("Resubscribe started, waiting for 2 seconds")
                        try {
                            channel.shutdownNow()
                        } catch (e: Exception) {
                            LOG.warn("Exception during shutdown", e)
                        }
                        Thread.sleep(5000)
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
            LOG.info("Resubscribed to app = ${watchState.value.appName} and lastVersion = ${watchState.value.lastVersion}")
        }
        LOG.info("Watch subscription is (re)initialized")
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
                LOG.warn("Unable to get initial configuration for seconds = $INITIAL_TIMEOUT_SEC seconds, retrying..")
            }
        }
        LOG.info("Initial properties for app = $appName received with size = ${initialProperties.size}")
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