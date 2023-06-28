package com.configset.client

import com.configset.client.proto.ConfigurationServiceGrpc
import com.configset.client.proto.PropertiesChangesResponse
import com.configset.client.proto.PropertyItem
import com.configset.client.proto.WatchRequest
import com.configset.common.client.extension.createLoggerStatic
import com.configset.common.client.grpc.ConfigSetClient
import com.configset.common.client.grpc.DeadlineInterceptor
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

private val log = createLoggerStatic<ClientTestUtil>()

class ClientTestUtil {

    lateinit var asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub
    private var updateVersion = AtomicLong(2)
    private lateinit var channel: ManagedChannel
    private lateinit var server: Server

    private val service = FakeConfigService()

    fun start() {
        server = InProcessServerBuilder.forName("mytest")
            .addService(service)
            .build()
            .start()
        channel = InProcessChannelBuilder.forName("mytest")
            .intercept(DeadlineInterceptor(60_000))
            .build()
        asyncClient = ConfigSetClient(channel).asyncClient
    }

    fun pushPropertyUpdate(
        appName: String,
        propertyName: String,
        propertyValue: String,
    ) {
        val version = updateVersion.incrementAndGet()
        service.pushMessageAsync(
            Payload.Msg(
                PropertiesChangesResponse.newBuilder()
                    .setApplicationName(appName)
                    .setLastVersion(version)
                    .addItems(
                        PropertyItem.newBuilder()
                            .setApplicationName(appName)
                            .setPropertyName(propertyName)
                            .setPropertyValue(propertyValue)
                            .setVersion(version)
                            .setDeleted(false)
                            .build()
                    )
                    .build()
            )
        )
    }

    fun pushPropertyDeleted(
        appName: String,
        propertyName: String,
    ) {
        val version = updateVersion.incrementAndGet()
        service.pushMessageAsync(
            Payload.Msg(
                PropertiesChangesResponse.newBuilder()
                    .setApplicationName(appName)
                    .setLastVersion(version)
                    .addItems(
                        PropertyItem.newBuilder()
                            .setApplicationName(appName)
                            .setPropertyName(propertyName)
                            .setVersion(version)
                            .setDeleted(true)
                            .build()
                    )
                    .build()
            )
        )
    }

    fun dropConnection() {
        service.pushMessageAsync(Payload.DropConnection)
    }

    fun stop() {
        channel.shutdown()
        server.shutdown()
    }
}

private class FakeConfigService : ConfigurationServiceGrpc.ConfigurationServiceImplBase() {

    private var subscribers = mutableListOf<Consumer<Payload>>()
    private var msgLog = mutableListOf<Payload.Msg>()
    private var executor = Executors.newCachedThreadPool(ThreadFactoryBuilder().setDaemon(true).build())

    @Synchronized
    fun pushMessageAsync(msg: Payload) {
        if (msg is Payload.Msg) {
            msgLog.add(msg)
        }
        executor.execute {
            subscribers.forEach { it.accept(msg) }
        }
    }

    @Synchronized
    fun addSubscriber(t: Consumer<Payload>) {
        subscribers.add(t)
    }

    override fun watchChanges(responseObserver: StreamObserver<PropertiesChangesResponse>):
            StreamObserver<WatchRequest> {

        val r = CompletingObserver(responseObserver, msgLog)
        addSubscriber(r)
        return r
    }
}

private class CompletingObserver(
    private val responseObserver: StreamObserver<PropertiesChangesResponse>,
    private val msgLog: List<Payload.Msg>,
) : StreamObserver<WatchRequest>, Consumer<Payload> {

    @Volatile
    private var completed = false
    private lateinit var subscriber: Consumer<Payload>
    private val subscribed = CountDownLatch(1)

    override fun accept(t: Payload) {
        subscribed.await()
        subscriber.accept(t)
    }

    override fun onNext(value: WatchRequest) {
        when (value.type) {
            WatchRequest.Type.SUBSCRIBE_APPLICATION -> {
                subscriber = Consumer<Payload> {
                    if (completed) {
                        return@Consumer
                    }
                    when (it) {
                        is Payload.DropConnection -> {
                            completed = true
                            responseObserver.onCompleted()
                        }

                        is Payload.Msg -> responseObserver.onNext(it.data)
                    }
                }
                startSubscription(subscriber)
                subscribed.countDown()
                for (payload in msgLog) {
                    subscriber.accept(payload)
                }
            }

            WatchRequest.Type.UPDATE_RECEIVED -> return
            else -> responseObserver.onError(RuntimeException("Unrecognized option received ${value.type}"))
        }
    }

    override fun onError(t: Throwable) {
        log.warn("Exception in stream", t)
        completed = true
    }

    override fun onCompleted() {
        completed = true
    }

    private fun startSubscription(subscriber: Consumer<Payload>) {
        subscriber.accept(
            Payload.Msg(
                PropertiesChangesResponse.newBuilder()
                    .setApplicationName(APP_NAME)
                    .setLastVersion(1)
                    .build()
            )
        )
    }
}

private sealed class Payload {
    object DropConnection : Payload()
    data class Msg(val data: PropertiesChangesResponse) : Payload()
}
