package com.configset.client

import com.configset.client.proto.ConfigurationServiceGrpc
import com.configset.client.proto.PropertiesChangesResponse
import com.configset.client.proto.PropertyItem
import com.configset.client.proto.WatchRequest
import com.configset.common.client.ConfigSetClient
import com.configset.common.client.DeadlineInterceptor
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

class ClientTestUtil {

    lateinit var asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub
    private var updateVersion = AtomicLong()
    private val cmdQueue = LinkedBlockingQueue<Payload>()
    private lateinit var channel: ManagedChannel
    private lateinit var server: Server

    private val service = object : ConfigurationServiceGrpc.ConfigurationServiceImplBase() {

        @Volatile
        var isShuttedDown = false

        override fun watchChanges(responseObserver: StreamObserver<PropertiesChangesResponse>):
                StreamObserver<WatchRequest> {

            return object : StreamObserver<WatchRequest> {
                override fun onNext(value: WatchRequest) {
                    when (value.type) {
                        WatchRequest.Type.SUBSCRIBE_APPLICATION -> startSubscription(responseObserver)
                        WatchRequest.Type.UPDATE_RECEIVED -> return
                        else -> responseObserver.onError(RuntimeException("Unrecognized option received"))
                    }
                    if (value.type != WatchRequest.Type.SUBSCRIBE_APPLICATION) {
                        return
                    }
                }

                override fun onError(t: Throwable) {
                    isShuttedDown = true
                }

                override fun onCompleted() {
                    isShuttedDown = true
                }

                private fun startSubscription(responseObserver: StreamObserver<PropertiesChangesResponse>) {
                    thread {
                        val lastVersion = if (!cmdQueue.isEmpty() && cmdQueue.element() is Payload.Msg) {
                            (cmdQueue.element() as Payload.Msg).data.lastVersion - 1
                        } else {
                            updateVersion.get()
                        }
                        responseObserver.onNext(
                            PropertiesChangesResponse.newBuilder()
                                .setApplicationName(APP_NAME)
                                .setLastVersion(lastVersion)
                                .build()
                        )
                        while (!isShuttedDown) {
                            when (val cmd = cmdQueue.take()) {
                                Payload.DropConnection -> {
                                    responseObserver.onCompleted()
                                    return@thread
                                }

                                is Payload.Msg -> responseObserver.onNext(cmd.data)
                            }
                        }
                    }
                }
            }
        }
    }

    fun start() {
        server = InProcessServerBuilder.forName("mytest")
            .addService(service)
            .build()
            .start()
        channel = InProcessChannelBuilder.forName("mytest")
            .intercept(DeadlineInterceptor(10_000))
            .build()
        asyncClient = ConfigSetClient(channel).asyncClient
    }

    fun pushPropertyUpdate(
        appName: String,
        propertyName: String,
        propertyValue: String,
    ) {

        val version = updateVersion.incrementAndGet()
        cmdQueue.add(
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
        cmdQueue.add(
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
        cmdQueue.add(Payload.DropConnection)
    }

    fun stop() {
        channel.shutdown()
        server.shutdown()
    }
}

private sealed class Payload {
    object DropConnection : Payload()
    data class Msg(val data: PropertiesChangesResponse) : Payload()
}
