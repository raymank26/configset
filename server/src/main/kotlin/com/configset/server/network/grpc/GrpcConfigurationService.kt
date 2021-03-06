package com.configset.server.network.grpc

import com.configset.sdk.extension.createLogger
import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.sdk.proto.ApplicationCreatedResponse
import com.configset.sdk.proto.ApplicationsResponse
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.sdk.proto.CreateHostRequest
import com.configset.sdk.proto.CreateHostResponse
import com.configset.sdk.proto.DeletePropertyRequest
import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.EmptyRequest
import com.configset.sdk.proto.ListHostsResponse
import com.configset.sdk.proto.ListPropertiesRequest
import com.configset.sdk.proto.ListPropertiesResponse
import com.configset.sdk.proto.PropertiesChangesResponse
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.ReadPropertyRequest
import com.configset.sdk.proto.ReadPropertyResponse
import com.configset.sdk.proto.SearchPropertiesRequest
import com.configset.sdk.proto.SearchPropertiesResponse
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import com.configset.sdk.proto.WatchRequest
import com.configset.server.ConfigurationService
import com.configset.server.CreateApplicationResult
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.PropertiesChanges
import com.configset.server.PropertyCreateResult
import com.configset.server.SearchPropertyRequest
import com.configset.server.WatchSubscriber
import io.grpc.stub.StreamObserver
import java.util.*


class GrpcConfigurationService(private val configurationService: ConfigurationService) : ConfigurationServiceGrpc.ConfigurationServiceImplBase() {

    private val log = createLogger()

    override fun createApplication(request: ApplicationCreateRequest, responseObserver: StreamObserver<ApplicationCreatedResponse>) {
        when (configurationService.createApplication(request.requestId, request.applicationName)) {
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
        responseObserver.onNext(ApplicationsResponse.newBuilder().addAllApplications(listApplications).build())
        responseObserver.onCompleted()
    }

    override fun createHost(request: CreateHostRequest, responseObserver: StreamObserver<CreateHostResponse>) {
        when (configurationService.createHost(request.requestId, request.hostName)) {
            HostCreateResult.OK -> responseObserver.onNext(CreateHostResponse.newBuilder().setType(CreateHostResponse.Type.OK).build())
            HostCreateResult.HostAlreadyExists -> responseObserver.onNext(CreateHostResponse.newBuilder().setType(CreateHostResponse.Type.OK).build())
        }
        responseObserver.onCompleted()
    }

    override fun listHosts(request: EmptyRequest, responseObserver: StreamObserver<ListHostsResponse>) {
        val builder = ListHostsResponse.newBuilder()
        for (host in configurationService.listHosts()) {
            builder.addHostNames(host.name)
        }
        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()
    }

    override fun updateProperty(request: UpdatePropertyRequest, responseObserver: StreamObserver<UpdatePropertyResponse>) {
        val version = if (request.version == 0L) null else request.version
        when (configurationService.updateProperty(request.requestId, request.applicationName, request.hostName, request.propertyName, request.propertyValue, version)) {
            PropertyCreateResult.OK -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.OK).build())
            PropertyCreateResult.HostNotFound -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.HOST_NOT_FOUND).build())
            PropertyCreateResult.ApplicationNotFound -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.APPLICATION_NOT_FOUND).build())
            PropertyCreateResult.UpdateConflict -> responseObserver.onNext(UpdatePropertyResponse.newBuilder().setType(UpdatePropertyResponse.Type.UPDATE_CONFLICT).build())
        }
        responseObserver.onCompleted()
    }

    override fun deleteProperty(request: DeletePropertyRequest, responseObserver: StreamObserver<DeletePropertyResponse>) {
        when (configurationService.deleteProperty(request.requestId, request.applicationName, request.hostName, request.propertyName, request.version)) {
            DeletePropertyResult.OK -> responseObserver.onNext(DeletePropertyResponse.newBuilder().setType(DeletePropertyResponse.Type.OK).build())
            DeletePropertyResult.PropertyNotFound -> responseObserver.onNext(DeletePropertyResponse.newBuilder().setType(DeletePropertyResponse.Type.PROPERTY_NOT_FOUND).build())
            DeletePropertyResult.DeleteConflict -> responseObserver.onNext(DeletePropertyResponse.newBuilder().setType(DeletePropertyResponse.Type.DELETE_CONFLICT).build())
        }
        responseObserver.onCompleted()
    }

    override fun searchProperties(request: SearchPropertiesRequest, responseObserver: StreamObserver<SearchPropertiesResponse>) {
        val applicationName = request.applicationName?.takeIf { it.isNotEmpty() }
        val propertyNameQuery = request.propertyName?.takeIf { it.isNotEmpty() }
        val propertyValueQuery = request.propertyValue?.takeIf { it.isNotEmpty() }
        val hostNameQuery = request.hostName?.takeIf { it.isNotEmpty() }

        val foundProperties: List<com.configset.server.PropertyItem.Updated> = configurationService.searchProperties(
                SearchPropertyRequest(applicationName, propertyNameQuery, propertyValueQuery, hostNameQuery))

        val searchItems = foundProperties.map { prop ->
            com.configset.sdk.proto.ShowPropertyItem.newBuilder()
                    .setHostName(prop.hostName)
                    .setApplicationName(prop.applicationName)
                    .setPropertyName(prop.name)
                    .setPropertyValue(prop.value)
                    .setVersion(prop.version)
                    .build()
        }
        val response = SearchPropertiesResponse.newBuilder().addAllItems(searchItems).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun listProperties(request: ListPropertiesRequest, responseObserver: StreamObserver<ListPropertiesResponse>) {
        val properties: List<String> = configurationService.listProperties(request.applicationName)
        responseObserver.onNext(ListPropertiesResponse.newBuilder().addAllPropertyNames(properties).build())
        responseObserver.onCompleted()
    }

    private fun toPropertiesChangesResponse(appName: String, changes: PropertiesChanges?): PropertiesChangesResponse {
        if (changes == null) {
            return PropertiesChangesResponse.newBuilder().setApplicationName(appName).build()
        }
        val preparedItems = changes.propertyItems.map { change ->
            convertPropertyItem(change)
        }
        return PropertiesChangesResponse.newBuilder().setApplicationName(appName).addAllItems(preparedItems)
                .setLastVersion(changes.lastVersion).build()
    }

    private fun convertPropertyItem(change: com.configset.server.PropertyItem): PropertyItem {
        val itemBuilder = PropertyItem.newBuilder()
                .setApplicationName(change.applicationName)
                .setPropertyName(change.name)
                .setVersion(change.version)
        return when (change) {
            is com.configset.server.PropertyItem.Updated -> {
                itemBuilder
                        .setUpdateType(PropertyItem.UpdateType.UPDATE)
                        .setPropertyValue(change.value)
                        .build()
            }
            is com.configset.server.PropertyItem.Deleted -> {
                itemBuilder
                        .setUpdateType(PropertyItem.UpdateType.DELETE)
                        .build()
            }
        }
    }

    override fun readProperty(request: ReadPropertyRequest, responseObserver: StreamObserver<ReadPropertyResponse>) {
        val propertyItem: com.configset.server.PropertyItem? = configurationService.readProperty(request.applicationName, request.hostName, request.propertyName)
        if (propertyItem == null) {
            responseObserver.onNext(ReadPropertyResponse.newBuilder().setHasItem(false).build())
        } else {
            responseObserver.onNext(ReadPropertyResponse.newBuilder().setHasItem(true).setItem(convertPropertyItem(propertyItem)).build())
        }
        responseObserver.onCompleted()
    }

    override fun watchChanges(responseObserver: StreamObserver<PropertiesChangesResponse>): StreamObserver<WatchRequest> {
        val subscriberId = UUID.randomUUID().toString()
        var subscribed = false
        return object : StreamObserver<WatchRequest> {
            override fun onNext(value: WatchRequest) {
                when (value.type) {
                    WatchRequest.Type.UPDATE_RECEIVED -> {
                        val updateReceived = value.updateReceived
                        configurationService.updateLastVersion(subscriberId, updateReceived.applicationName, updateReceived.version)
                    }
                    WatchRequest.Type.SUBSCRIBE_APPLICATION -> {
                        val subscribeRequest = value.subscribeApplicationRequest
                        log.debug("Subscriber with id = $subscriberId call subscribe for app = ${subscribeRequest.applicationName}" +
                                ", lastVersion = ${subscribeRequest.lastKnownVersion}" +
                                ", hostName = ${subscribeRequest.hostName}")
                        val changesToPush = configurationService.subscribeApplication(subscriberId, subscribeRequest.defaultApplicationName, subscribeRequest.hostName,
                                subscribeRequest.applicationName, subscribeRequest.lastKnownVersion)
                        if (!subscribed) {
                            configurationService.watchChanges(object : WatchSubscriber {
                                override fun getId(): String {
                                    return subscriberId
                                }

                                override fun pushChanges(applicationName: String, changes: PropertiesChanges) {
                                    responseObserver.onNext(toPropertiesChangesResponse(applicationName, changes))
                                }
                            })
                            subscribed = true
                        }
                        responseObserver.onNext(toPropertiesChangesResponse(subscribeRequest.applicationName, changesToPush))
                    }
                    else -> log.warn("Unrecognized message type = ${value.type}")
                }
            }

            override fun onError(t: Throwable?) {
                log.warn("Error in incoming stream, the bidirectional stream will be closed, unsubscribe will be called", t)
                configurationService.unsubscribe(subscriberId)
            }

            override fun onCompleted() {
                configurationService.unsubscribe(subscriberId)
                responseObserver.onCompleted()
            }
        }
    }
}