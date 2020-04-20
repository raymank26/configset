package com.letsconfig.client.repository.grpc

import com.letsconfig.client.ChangingObservable
import com.letsconfig.client.DynamicValue
import com.letsconfig.client.PropertyItem
import com.letsconfig.client.Subscriber
import com.letsconfig.client.repository.ConfigurationRepository
import com.letsconfig.sdk.extension.createLogger
import com.letsconfig.sdk.proto.ConfigurationServiceGrpc
import com.letsconfig.sdk.proto.SubscribeApplicationRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class GrpcConfigurationRepository(
        private val applicationHostname: String,
        private val serverHostname: String,
        private val serverPort: Int
) : ConfigurationRepository {

    private val log = createLogger()
    private lateinit var asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub
    private lateinit var blockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    private lateinit var channel: ManagedChannel
    private lateinit var subscribeObserver: StreamObserver<SubscribeApplicationRequest>
    private lateinit var watchObserver: WatchObserver
    private val appWatchMappers = HashMap<String, WatchState>()

    @Synchronized
    override fun start() {
        channel = ManagedChannelBuilder.forAddress(serverHostname, serverPort)
                .usePlaintext()
                .build()
        asyncClient = ConfigurationServiceGrpc.newStub(channel)
        blockingClient = ConfigurationServiceGrpc.newBlockingStub(channel)

        watchObserver = WatchObserver(
                onUpdate = { appName, updates, lastVersion ->
                    synchronized(this) {
                        val watchState: WatchState = appWatchMappers[appName]!!
                        watchState.observable.setValue(updates)
                        watchState.lastVersion = lastVersion
                    }
                },
                onEnd = {
                    thread {
                        log.info("Resubscribe initialized, waiting for 2 seconds")
                        Thread.sleep(2000)
                        subscribe()
                    }
                })
        subscribe()
    }

    @Synchronized
    private fun subscribe() {
        subscribeObserver = asyncClient.watchChanges(watchObserver)
        for (watchState in appWatchMappers) {
            subscribeObserver.onNext(SubscribeApplicationRequest
                    .newBuilder()
                    .setApplicationName(watchState.value.appName)
                    .setHostName(applicationHostname)
                    .setLastKnownVersion(watchState.value.lastVersion)
                    .build())
            log.info("Resubscribed to app = ${watchState.value.appName} and lastVersion = ${watchState.value.lastVersion}")
        }
        log.info("Watch subscription is (re)initialized")
    }

    @Synchronized
    override fun subscribeToProperties(appName: String): DynamicValue<List<PropertyItem.Updated>, List<PropertyItem>> {
        val currentObservable = ChangingObservable<List<PropertyItem>>()
        appWatchMappers[appName] = WatchState(appName, 0, currentObservable)
        subscribeObserver.onNext(SubscribeApplicationRequest
                .newBuilder()
                .setApplicationName(appName)
                .setHostName(applicationHostname)
                .build())

        val future = CompletableFuture<List<PropertyItem.Updated>>()
        currentObservable.onSubscribe(object : Subscriber<List<PropertyItem>> {
            override fun process(value: List<PropertyItem>) {
                val snapshot = value.filterIsInstance<PropertyItem.Updated>()
                        .map { PropertyItem.Updated(it.applicationName, it.name, it.version, it.value) }
                future.complete(snapshot)
            }
        })
        return DynamicValue(future.get(30, TimeUnit.SECONDS), currentObservable)
    }

    @Synchronized
    override fun stop() {
        subscribeObserver.onCompleted()
        watchObserver.isStopped = true
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
    }
}

private data class WatchState(val appName: String, var lastVersion: Long, val observable: ChangingObservable<List<PropertyItem>>)