package com.letsconfig.server.network.grpc

import com.letsconfig.sdk.extension.createLogger
import com.letsconfig.sdk.proto.ApplicationCreateRequest
import com.letsconfig.sdk.proto.ApplicationCreatedResponse
import com.letsconfig.sdk.proto.ApplicationsResponse
import com.letsconfig.sdk.proto.ConfigurationServiceGrpc
import com.letsconfig.sdk.proto.CreateHostRequest
import com.letsconfig.sdk.proto.CreateHostResponse
import com.letsconfig.sdk.proto.DeletePropertyRequest
import com.letsconfig.sdk.proto.DeletePropertyResponse
import com.letsconfig.sdk.proto.EmptyRequest
import com.letsconfig.sdk.proto.ListHostsResponse
import com.letsconfig.sdk.proto.ListPropertiesRequest
import com.letsconfig.sdk.proto.ListPropertiesResponse
import com.letsconfig.sdk.proto.PropertiesChangesResponse
import com.letsconfig.sdk.proto.PropertyItem
import com.letsconfig.sdk.proto.SearchPropertiesRequest
import com.letsconfig.sdk.proto.SearchPropertiesResponse
import com.letsconfig.sdk.proto.SearchResponseItem
import com.letsconfig.sdk.proto.ShowPropertyRequest
import com.letsconfig.sdk.proto.ShowPropertyResponse
import com.letsconfig.sdk.proto.UpdatePropertyRequest
import com.letsconfig.sdk.proto.UpdatePropertyResponse
import com.letsconfig.sdk.proto.WatchRequest
import com.letsconfig.server.ConfigurationService
import com.letsconfig.server.CreateApplicationResult
import com.letsconfig.server.DeletePropertyResult
import com.letsconfig.server.HostCreateResult
import com.letsconfig.server.PropertiesChanges
import com.letsconfig.server.PropertyCreateResult
import com.letsconfig.server.SearchPropertyRequest
import com.letsconfig.server.ShowPropertyItem
import com.letsconfig.server.WatchSubscriber
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
        when (configurationService.deleteProperty(request.requestId, request.applicationName, request.hostName, request.propertyName)) {
            DeletePropertyResult.OK -> responseObserver.onNext(DeletePropertyResponse.newBuilder().setType(DeletePropertyResponse.Type.OK).build())
            DeletePropertyResult.PropertyNotFound -> responseObserver.onNext(DeletePropertyResponse.newBuilder().setType(DeletePropertyResponse.Type.PROPERTY_NOT_FOUND).build())
        }
        responseObserver.onCompleted()
    }

    override fun searchProperties(request: SearchPropertiesRequest, responseObserver: StreamObserver<SearchPropertiesResponse>) {
        val applicationName = request.applicationName?.takeIf { it.isNotEmpty() }
        val propertyNameQuery = request.propertyName?.takeIf { it.isNotEmpty() }
        val propertyValueQuery = request.propertyValue?.takeIf { it.isNotEmpty() }
        val hostNameQuery = request.hostName?.takeIf { it.isNotEmpty() }

        val foundProperties: Map<String, List<String>> = configurationService.searchProperties(
                SearchPropertyRequest(applicationName, propertyNameQuery, propertyValueQuery, hostNameQuery))

        val searchItems: List<SearchResponseItem> = foundProperties.map { (appName, properties) ->
            val itemBuilder = SearchResponseItem.newBuilder().setAppName(appName)
            for (property in properties) {
                itemBuilder.addPropertyNames(property)
            }
            itemBuilder.build()
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

    override fun showProperty(request: ShowPropertyRequest, responseObserver: StreamObserver<ShowPropertyResponse>) {
        val res: List<ShowPropertyItem> = configurationService.showProperty(request.applicationName, request.propertyName)
        val protoItems = res.map {
            com.letsconfig.sdk.proto.ShowPropertyItem.newBuilder()
                    .setHostName(it.hostName)
                    .setPropertyName(it.propertyValue)
                    .setPropertyValue(it.propertyValue)
                    .build()
        }
        responseObserver.onNext(ShowPropertyResponse.newBuilder().addAllItems(protoItems).build())
    }

    private fun toPropertiesChangesResponse(appName: String, changes: PropertiesChanges?): PropertiesChangesResponse {
        if (changes == null) {
            return PropertiesChangesResponse.newBuilder().setApplicationName(appName).build()
        }
        val preparedItems = changes.propertyItems.map { change ->
            val itemBuilder = PropertyItem.newBuilder()
                    .setApplicationName(change.applicationName)
                    .setPropertyName(change.name)
                    .setVersion(change.version)
            when (change) {
                is com.letsconfig.server.PropertyItem.Updated -> {
                    itemBuilder
                            .setUpdateType(PropertyItem.UpdateType.UPDATE)
                            .setPropertyValue(change.value)
                            .build()
                }
                is com.letsconfig.server.PropertyItem.Deleted -> {
                    itemBuilder
                            .setUpdateType(PropertyItem.UpdateType.DELETE)
                            .build()
                }
            }
        }
        return PropertiesChangesResponse.newBuilder().setApplicationName(appName).addAllItems(preparedItems)
                .setLastVersion(changes.lastVersion).build()
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
                                " and lastVersion = ${subscribeRequest.lastKnownVersion}")
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