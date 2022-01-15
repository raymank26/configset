package com.configset.client

import com.configset.sdk.client.ConfigSetClient
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.sdk.proto.PropertiesChangesResponse
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.WatchRequest
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcCleanupRule
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread


class ClientTestUtil(grpcCleanup: GrpcCleanupRule) {

    var asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub
    private var updateVersion = AtomicLong()
    private val cmdQueue = LinkedBlockingQueue<Payload>()

    private val service = object : ConfigurationServiceGrpc.ConfigurationServiceImplBase() {
        override fun watchChanges(responseObserver: StreamObserver<PropertiesChangesResponse>): StreamObserver<WatchRequest> {
            var isShuttedDown = false

            return object : StreamObserver<WatchRequest> {
                override fun onNext(value: WatchRequest) {
                    if (value.type != WatchRequest.Type.SUBSCRIBE_APPLICATION) {
                        return
                    }
                    thread {
                        val lastVersion = if (!cmdQueue.isEmpty() && cmdQueue.element() is Payload.Msg) {
                            (cmdQueue.element() as Payload.Msg).data.lastVersion - 1
                        } else {
                            updateVersion.get()
                        }
                        responseObserver.onNext(PropertiesChangesResponse.newBuilder()
                            .setApplicationName(APP_NAME)
                            .setLastVersion(lastVersion)
                            .build())
                        while (!isShuttedDown) {
                            when (val cmd = cmdQueue.take()) {
                                Payload.DropConnection -> {
                                    isShuttedDown = true
                                    responseObserver.onCompleted()
                                }
                                is Payload.Msg -> responseObserver.onNext(cmd.data)
                            }
                        }
                    }
                }

                override fun onError(t: Throwable) {
                    isShuttedDown = true
                }

                override fun onCompleted() {
                    isShuttedDown = true
                }
            }
        }
    }

    init {
        grpcCleanup.register(InProcessServerBuilder.forName("mytest")
            .directExecutor().addService(service).build().start())
        val channel = grpcCleanup.register(InProcessChannelBuilder.forName("mytest").directExecutor().build())
        asyncClient = ConfigSetClient(channel).asyncClient
    }

    fun pushPropertyUpdate(
        appName: String,
        propertyName: String,
        propertyValue: String,
    ) {

        val version = updateVersion.incrementAndGet()
        cmdQueue.add(Payload.Msg(PropertiesChangesResponse.newBuilder()
            .setApplicationName(appName)
            .setLastVersion(version)
            .addItems(PropertyItem.newBuilder()
                .setApplicationName(appName)
                .setPropertyName(propertyName)
                .setPropertyValue(propertyValue)
                .setVersion(version)
                .setUpdateType(PropertyItem.UpdateType.UPDATE)
                .build())
            .build()
        ))
    }

    fun pushPropertyDeleted(
        appName: String,
        propertyName: String,
    ) {

        val version = updateVersion.incrementAndGet()
        cmdQueue.add(Payload.Msg(PropertiesChangesResponse.newBuilder()
            .setApplicationName(appName)
            .setLastVersion(version)
            .addItems(PropertyItem.newBuilder()
                .setApplicationName(appName)
                .setPropertyName(propertyName)
                .setVersion(version)
                .setUpdateType(PropertyItem.UpdateType.DELETE)
                .build())
            .build())
        )
    }

    fun dropConnection() {
        cmdQueue.add(Payload.DropConnection)
    }
}

private sealed class Payload {
    object DropConnection : Payload()
    data class Msg(val data: PropertiesChangesResponse) : Payload()
}