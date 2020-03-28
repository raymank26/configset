package com.letsconfig.network.grpc

import com.letsconfig.network.CreateApplicationResult
import com.letsconfig.network.DeletePropertyResult
import com.letsconfig.network.HostCreateResult
import com.letsconfig.network.NetworkApi
import com.letsconfig.network.PropertyCreateResult
import com.letsconfig.network.WatchSubscriber
import com.letsconfig.network.grpc.common.ApplicationCreatedResponse
import com.letsconfig.network.grpc.common.ApplicationRequest
import com.letsconfig.network.grpc.common.ApplicationSnapshotResponse
import com.letsconfig.network.grpc.common.ApplicationsResponse
import com.letsconfig.network.grpc.common.ConfigurationServiceGrpc
import com.letsconfig.network.grpc.common.CreateHostRequest
import com.letsconfig.network.grpc.common.CreateHostResponse
import com.letsconfig.network.grpc.common.DeletePropertyRequest
import com.letsconfig.network.grpc.common.DeletePropertyResponse
import com.letsconfig.network.grpc.common.EmptyRequest
import com.letsconfig.network.grpc.common.PropertyItem
import com.letsconfig.network.grpc.common.SubscribeApplicationRequest
import com.letsconfig.network.grpc.common.SubscriberInfoRequest
import com.letsconfig.network.grpc.common.UpdatePropertyRequest
import com.letsconfig.network.grpc.common.UpdatePropertyResponse
import io.grpc.Context
import io.grpc.stub.StreamObserver

class GrpcConfigurationService(private val networkApi: NetworkApi) : ConfigurationServiceGrpc.ConfigurationServiceImplBase() {

    override fun createApplication(request: ApplicationRequest, responseObserver: StreamObserver<ApplicationCreatedResponse>) {
        when (networkApi.createApplication(request.applicationName)) {
            CreateApplicationResult.OK -> {
                responseObserver.onNext(ApplicationCreatedResponse.newBuilder()
                        .setType(ApplicationCreatedResponse.Type.OK)
                        .build())
            }
            CreateApplicationResult.ApplicationAlreadyExists -> {
                responseObserver.onNext(ApplicationCreatedResponse.newBuilder()
                        .setType(ApplicationCreatedResponse.Type.ALREADY_EXISTS)
                        .build())
            }
        }
        responseObserver.onCompleted()
    }

    override fun listApplications(request: EmptyRequest, responseObserver: StreamObserver<ApplicationsResponse>) {
        responseObserver.onNext(ApplicationsResponse.newBuilder().addAllApplication(networkApi.listApplications()).build())
        responseObserver.onCompleted()
    }

    override fun createHost(request: CreateHostRequest, responseObserver: StreamObserver<CreateHostResponse>) {
        when (networkApi.createHost(request.hostName)) {
            HostCreateResult.OK -> responseObserver.onNext(CreateHostResponse.newBuilder().setType(CreateHostResponse.Type.OK).build())
            HostCreateResult.HostAlreadyExists -> responseObserver.onNext(CreateHostResponse.newBuilder().setType(CreateHostResponse.Type.OK).build())
        }
        responseObserver.onCompleted()
    }

    override fun updateProperty(request: UpdatePropertyRequest, responseObserver: StreamObserver<UpdatePropertyResponse>) {
        when (networkApi.updateProperty(request.applicationName, request.hostName, request.propertyName, request.propertyValue)) {
            PropertyCreateResult.OK -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.OK).build())
            PropertyCreateResult.HostNotFound -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.HOST_NOT_FOUND).build())
            PropertyCreateResult.ApplicationNotFound -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.APPLICATION_NOT_FOUND).build())
        }
        responseObserver.onCompleted()
    }

    override fun deleteProperty(request: DeletePropertyRequest, responseObserver: StreamObserver<DeletePropertyResponse>) {
        when (networkApi.deleteProperty(request.applicationName, request.hostName, request.propertyName)) {
            DeletePropertyResult.OK -> responseObserver.onNext(DeletePropertyResponse.newBuilder().setType(DeletePropertyResponse.Type.OK).build())
            DeletePropertyResult.HostNotFound -> responseObserver.onNext(DeletePropertyResponse.newBuilder().setType(DeletePropertyResponse.Type.HOST_NOT_FOUND).build())
            DeletePropertyResult.ApplicationNotFound -> responseObserver.onNext(DeletePropertyResponse.newBuilder().setType(DeletePropertyResponse.Type.APPLICATION_NOT_FOUND).build())
        }
        responseObserver.onCompleted()
    }

    override fun subscribeApplication(request: SubscribeApplicationRequest, responseObserver: StreamObserver<ApplicationSnapshotResponse>) {
        val properties: List<com.letsconfig.network.PropertyItem.Updated> = networkApi.subscribeApplication(request.subscriberId, request.hostName, request.applicationName)
        responseObserver.onNext(ApplicationSnapshotResponse.newBuilder().addAllItems(properties.map { prop ->
            PropertyItem.newBuilder()
                    .setUpdateType(PropertyItem.UpdateType.UPDATE)
                    .setApplicationName(prop.applicationName)
                    .setPropertyName(prop.name)
                    .setPropertyValue(prop.value)
                    .setVersion(prop.version)
                    .build()
        }).build())
        responseObserver.onCompleted()
    }

    override fun watchChanges(request: SubscriberInfoRequest, responseObserver: StreamObserver<PropertyItem>) {
        networkApi.watchChanges(object : WatchSubscriber {
            override fun getId(): String {
                return request.id
            }

            override fun pushChanges(change: com.letsconfig.network.PropertyItem) {
                if (Context.current().isCancelled) {
                    networkApi.unsubscribe(request.id)
                }
                when (change) {
                    is com.letsconfig.network.PropertyItem.Updated -> {
                        responseObserver.onNext(PropertyItem.newBuilder()
                                .setUpdateType(PropertyItem.UpdateType.UPDATE)
                                .setApplicationName(change.applicationName)
                                .setPropertyName(change.name)
                                .setPropertyValue(change.value)
                                .setVersion(change.version)
                                .build()
                        )
                    }
                    is com.letsconfig.network.PropertyItem.Deleted -> {
                        responseObserver.onNext(PropertyItem.newBuilder()
                                .setUpdateType(PropertyItem.UpdateType.DELETE)
                                .setApplicationName(change.applicationName)
                                .setPropertyName(change.name)
                                .setVersion(change.version)
                                .build())
                    }
                }
            }
        })
    }
}