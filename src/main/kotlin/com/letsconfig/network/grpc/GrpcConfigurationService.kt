package com.letsconfig.network.grpc

import com.letsconfig.ConfigurationService
import com.letsconfig.CreateApplicationResult
import com.letsconfig.DeletePropertyResult
import com.letsconfig.HostCreateResult
import com.letsconfig.PropertyCreateResult
import com.letsconfig.WatchSubscriber
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

class GrpcConfigurationService(private val configurationService: ConfigurationService) : ConfigurationServiceGrpc.ConfigurationServiceImplBase() {

    override fun createApplication(request: ApplicationRequest, responseObserver: StreamObserver<ApplicationCreatedResponse>) {
        when (configurationService.createApplication(request.applicationName)) {
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
        val listApplications = configurationService.listApplications().map { it.name }
        responseObserver.onNext(ApplicationsResponse.newBuilder().addAllApplication(listApplications).build())
        responseObserver.onCompleted()
    }

    override fun createHost(request: CreateHostRequest, responseObserver: StreamObserver<CreateHostResponse>) {
        when (configurationService.createHost(request.hostName)) {
            HostCreateResult.OK -> responseObserver.onNext(CreateHostResponse.newBuilder().setType(CreateHostResponse.Type.OK).build())
            HostCreateResult.HostAlreadyExists -> responseObserver.onNext(CreateHostResponse.newBuilder().setType(CreateHostResponse.Type.OK).build())
        }
        responseObserver.onCompleted()
    }

    override fun updateProperty(request: UpdatePropertyRequest, responseObserver: StreamObserver<UpdatePropertyResponse>) {
        val version = if (request.version == 0L) null else request.version
        when (configurationService.updateProperty(request.applicationName, request.hostName, request.propertyName, request.propertyValue, version)) {
            PropertyCreateResult.OK -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.OK).build())
            PropertyCreateResult.HostNotFound -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.HOST_NOT_FOUND).build())
            PropertyCreateResult.ApplicationNotFound -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.APPLICATION_NOT_FOUND).build())
            PropertyCreateResult.UpdateConflict -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.UPDATE_CONFLICT).build())
        }
        responseObserver.onCompleted()
    }

    override fun deleteProperty(request: DeletePropertyRequest, responseObserver: StreamObserver<DeletePropertyResponse>) {
        when (configurationService.deleteProperty(request.applicationName, request.hostName, request.propertyName)) {
            DeletePropertyResult.OK -> responseObserver.onNext(DeletePropertyResponse.newBuilder().setType(DeletePropertyResponse.Type.OK).build())
            DeletePropertyResult.PropertyNotFound -> responseObserver.onNext(DeletePropertyResponse.newBuilder().setType(DeletePropertyResponse.Type.PROPERTY_NOT_FOUND).build())
        }
        responseObserver.onCompleted()
    }

    override fun subscribeApplication(request: SubscribeApplicationRequest, responseObserver: StreamObserver<ApplicationSnapshotResponse>) {
        val properties: List<com.letsconfig.PropertyItem> = configurationService.subscribeApplication(request.subscriberId,
                request.defaultApplicationName, request.hostName, request.applicationName, request.lastKnownVersion)
        responseObserver.onNext(ApplicationSnapshotResponse.newBuilder().addAllItems(properties.map { prop ->
            val res = PropertyItem.newBuilder()
                    .setApplicationName(prop.applicationName)
                    .setPropertyName(prop.name)
                    .setVersion(prop.version)
            when (prop) {
                is com.letsconfig.PropertyItem.Updated -> {
                    res.propertyValue = prop.value
                    res.updateType = PropertyItem.UpdateType.UPDATE
                }
                is com.letsconfig.PropertyItem.Deleted -> {
                    res.updateType = PropertyItem.UpdateType.DELETE
                }
            }
            res.build()
        }).build())
        responseObserver.onCompleted()
    }

    override fun watchChanges(request: SubscriberInfoRequest, responseObserver: StreamObserver<PropertyItem>) {
        configurationService.watchChanges(object : WatchSubscriber {
            override fun getId(): String {
                return request.id
            }

            override fun pushChanges(change: com.letsconfig.PropertyItem) {
                if (Context.current().isCancelled) {
                    configurationService.unsubscribe(request.id)
                }
                when (change) {
                    is com.letsconfig.PropertyItem.Updated -> {
                        responseObserver.onNext(PropertyItem.newBuilder()
                                .setUpdateType(PropertyItem.UpdateType.UPDATE)
                                .setApplicationName(change.applicationName)
                                .setPropertyName(change.name)
                                .setPropertyValue(change.value)
                                .setVersion(change.version)
                                .build()
                        )
                    }
                    is com.letsconfig.PropertyItem.Deleted -> {
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