package com.configset.server.network.grpc

import com.configset.client.proto.Application
import com.configset.client.proto.ApplicationCreateRequest
import com.configset.client.proto.ApplicationCreatedResponse
import com.configset.client.proto.ApplicationDeleteRequest
import com.configset.client.proto.ApplicationDeletedResponse
import com.configset.client.proto.ApplicationUpdateRequest
import com.configset.client.proto.ApplicationUpdatedResponse
import com.configset.client.proto.ApplicationsResponse
import com.configset.client.proto.ConfigurationServiceGrpc
import com.configset.client.proto.CreateHostRequest
import com.configset.client.proto.CreateHostResponse
import com.configset.client.proto.DeletePropertyRequest
import com.configset.client.proto.DeletePropertyResponse
import com.configset.client.proto.EmptyRequest
import com.configset.client.proto.ListHostsResponse
import com.configset.client.proto.ListPropertiesRequest
import com.configset.client.proto.ListPropertiesResponse
import com.configset.client.proto.PropertiesChangesResponse
import com.configset.client.proto.PropertyItem
import com.configset.client.proto.ReadPropertyRequest
import com.configset.client.proto.ReadPropertyResponse
import com.configset.client.proto.SearchPropertiesRequest
import com.configset.client.proto.SearchPropertiesResponse
import com.configset.client.proto.UpdatePropertyRequest
import com.configset.client.proto.UpdatePropertyResponse
import com.configset.client.proto.WatchRequest
import com.configset.common.backend.auth.Admin
import com.configset.common.backend.auth.ApplicationOwner
import com.configset.common.backend.auth.HostCreator
import com.configset.common.backend.auth.Role
import com.configset.common.client.ApplicationId
import com.configset.common.client.extension.createLogger
import com.configset.server.ConfigurationService
import com.configset.server.CreateApplicationResult
import com.configset.server.DeleteApplicationResult
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.PropertiesChanges
import com.configset.server.PropertyCreateResult
import com.configset.server.SearchPropertyRequest
import com.configset.server.UpdateApplicationResult
import com.configset.server.WatchSubscriber
import com.configset.server.db.PropertyItemED
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import java.util.UUID

class GrpcConfigurationService(
    private val configurationService: ConfigurationService,
) : ConfigurationServiceGrpc.ConfigurationServiceImplBase() {

    private val log = createLogger()

    override fun createApplication(
        request: ApplicationCreateRequest,
        responseObserver: StreamObserver<ApplicationCreatedResponse>,
    ) {
        requireRole(Admin)
        when (configurationService.createApplication(request.requestId, request.applicationName)) {
            CreateApplicationResult.OK -> {
                responseObserver.onNext(
                    ApplicationCreatedResponse.newBuilder()
                        .setType(ApplicationCreatedResponse.Type.OK)
                        .build()
                )
            }

            CreateApplicationResult.ApplicationAlreadyExists -> {
                responseObserver.onNext(
                    ApplicationCreatedResponse.newBuilder()
                        .setType(ApplicationCreatedResponse.Type.ALREADY_EXISTS)
                        .build()
                )
            }
        }
        responseObserver.onCompleted()
    }

    override fun deleteApplication(
        request: ApplicationDeleteRequest,
        responseObserver: StreamObserver<ApplicationDeletedResponse>
    ) {
        requireRole(Admin)
        when (configurationService.deleteApplication(request.requestId, request.applicationName)) {
            DeleteApplicationResult.ApplicationNotFound -> responseObserver.onNext(
                ApplicationDeletedResponse.newBuilder()
                    .setType(ApplicationDeletedResponse.Type.APPLICATION_NOT_FOUND)
                    .build()
            )

            DeleteApplicationResult.OK -> responseObserver.onNext(
                ApplicationDeletedResponse.newBuilder()
                    .setType(ApplicationDeletedResponse.Type.OK)
                    .build()
            )
        }
        responseObserver.onCompleted()
    }

    override fun listApplications(request: EmptyRequest, responseObserver: StreamObserver<ApplicationsResponse>) {
        val responseBuilder = ApplicationsResponse.newBuilder()
        configurationService.listApplications().forEach {
            responseBuilder.addApplications(
                Application.newBuilder()
                    .setId(it.id.id.toString())
                    .setApplicationName(it.name)
                    .build()
            )
        }
        responseObserver.onNext(responseBuilder.build())
        responseObserver.onCompleted()
    }

    override fun updateApplication(
        request: ApplicationUpdateRequest,
        responseObserver: StreamObserver<ApplicationUpdatedResponse>
    ) {
        when (
            configurationService.updateApplication(
                request.requestId,
                ApplicationId(request.id),
                request.applicationName
            )
        ) {
            UpdateApplicationResult.ApplicationNotFound -> responseObserver.onNext(
                ApplicationUpdatedResponse.newBuilder()
                    .setType(ApplicationUpdatedResponse.Type.APPLICATION_NOT_FOUND)
                    .build()
            )

            UpdateApplicationResult.OK -> responseObserver.onNext(
                ApplicationUpdatedResponse.newBuilder()
                    .setType(ApplicationUpdatedResponse.Type.OK)
                    .build()
            )
        }
        responseObserver.onCompleted()
    }

    override fun createHost(request: CreateHostRequest, responseObserver: StreamObserver<CreateHostResponse>) {
        requireRole(HostCreator)
        when (configurationService.createHost(request.requestId, request.hostName)) {
            HostCreateResult.OK -> responseObserver.onNext(
                CreateHostResponse.newBuilder()
                    .setType(CreateHostResponse.Type.OK)
                    .build()
            )

            HostCreateResult.HostAlreadyExists -> responseObserver.onNext(
                CreateHostResponse.newBuilder()
                    .setType(CreateHostResponse.Type.OK)
                    .build()
            )
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

    override fun updateProperty(
        request: UpdatePropertyRequest,
        responseObserver: StreamObserver<UpdatePropertyResponse>,
    ) {
        requireRole(ApplicationOwner(request.applicationName))
        val version = if (request.version == 0L) null else request.version
        when (
            configurationService.updateProperty(
                request.requestId,
                request.applicationName,
                request.hostName,
                request.propertyName,
                request.propertyValue,
                version
            )
        ) {
            PropertyCreateResult.OK -> responseObserver.onNext(
                UpdatePropertyResponse.newBuilder()
                    .setType(UpdatePropertyResponse.Type.OK)
                    .build()
            )

            PropertyCreateResult.HostNotFound -> responseObserver.onNext(
                UpdatePropertyResponse.newBuilder()
                    .setType(UpdatePropertyResponse.Type.HOST_NOT_FOUND)
                    .build()
            )

            PropertyCreateResult.ApplicationNotFound -> responseObserver.onNext(
                UpdatePropertyResponse.newBuilder()
                    .setType(UpdatePropertyResponse.Type.APPLICATION_NOT_FOUND)
                    .build()
            )

            PropertyCreateResult.UpdateConflict -> responseObserver.onNext(
                UpdatePropertyResponse.newBuilder()
                    .setType(UpdatePropertyResponse.Type.UPDATE_CONFLICT)
                    .build()
            )
        }
        responseObserver.onCompleted()
    }

    override fun deleteProperty(
        request: DeletePropertyRequest,
        responseObserver: StreamObserver<DeletePropertyResponse>,
    ) {
        requireRole(ApplicationOwner(request.applicationName))
        when (
            configurationService.deleteProperty(
                request.requestId,
                request.applicationName,
                request.hostName,
                request.propertyName,
                request.version
            )
        ) {
            DeletePropertyResult.OK -> responseObserver.onNext(
                DeletePropertyResponse.newBuilder()
                    .setType(DeletePropertyResponse.Type.OK)
                    .build()
            )

            DeletePropertyResult.PropertyNotFound -> responseObserver.onNext(
                DeletePropertyResponse.newBuilder()
                    .setType(DeletePropertyResponse.Type.PROPERTY_NOT_FOUND)
                    .build()
            )

            DeletePropertyResult.DeleteConflict -> responseObserver.onNext(
                DeletePropertyResponse.newBuilder()
                    .setType(DeletePropertyResponse.Type.DELETE_CONFLICT)
                    .build()
            )
        }
        responseObserver.onCompleted()
    }

    override fun searchProperties(request: SearchPropertiesRequest, responseObserver: StreamObserver<SearchPropertiesResponse>) {
        val applicationName = request.applicationName?.takeIf { it.isNotEmpty() }
        val propertyNameQuery = request.propertyName?.takeIf { it.isNotEmpty() }
        val propertyValueQuery = request.propertyValue?.takeIf { it.isNotEmpty() }
        val hostNameQuery = request.hostName?.takeIf { it.isNotEmpty() }

        val foundProperties = configurationService.searchProperties(
            SearchPropertyRequest(
                applicationName,
                propertyNameQuery, propertyValueQuery, hostNameQuery
            )
        )

        val searchItems = foundProperties.map { prop ->
            convertPropertyItem(prop)
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

    private fun convertPropertyItem(item: PropertyItemED): PropertyItem {
        return PropertyItem.newBuilder()
            .setId(item.id!!.toString())
            .setApplicationName(item.applicationName)
            .setPropertyName(item.name)
            .setPropertyValue(item.value)
            .setVersion(item.version)
            .setDeleted(item.deleted)
            .setHostName(item.hostName)
            .build()
    }

    override fun readProperty(request: ReadPropertyRequest, responseObserver: StreamObserver<ReadPropertyResponse>) {
        val propertyItem = configurationService.readProperty(
            request.applicationName,
            request.hostName,
            request.propertyName
        )
        if (propertyItem == null) {
            responseObserver.onNext(
                ReadPropertyResponse.newBuilder()
                    .setHasItem(false)
                    .build()
            )
        } else {
            responseObserver.onNext(
                ReadPropertyResponse.newBuilder()
                    .setHasItem(true)
                    .setItem(convertPropertyItem(propertyItem))
                    .build()
            )
        }
        responseObserver.onCompleted()
    }

    override fun watchChanges(responseObserver: StreamObserver<PropertiesChangesResponse>): StreamObserver<WatchRequest> {
        val subscriberId = UUID.randomUUID().toString()
        return object : StreamObserver<WatchRequest> {
            override fun onNext(value: WatchRequest) {
                when (value.type) {
                    WatchRequest.Type.UPDATE_RECEIVED -> {
                        val updateReceived = value.updateReceived
                        configurationService.updateLastVersion(
                            subscriberId,
                            updateReceived.applicationName,
                            updateReceived.version
                        )
                    }
                    WatchRequest.Type.SUBSCRIBE_APPLICATION -> {
                        val subscribeRequest = value.subscribeApplicationRequest
                        log.debug(
                            """Subscriber with id = $subscriberId calls subscribe for 
                            |app = ${subscribeRequest.applicationName},
                            |lastVersion = ${subscribeRequest.lastKnownVersion},
                            |hostName = ${subscribeRequest.hostName}
                            """.trimMargin()
                        )
                        val subscriber = object : WatchSubscriber {
                            override fun getId(): String {
                                return subscriberId
                            }

                            override fun pushChanges(applicationName: String, changes: PropertiesChanges) {
                                responseObserver.onNext(toPropertiesChangesResponse(applicationName, changes))
                            }
                        }
                        val changesToPush = configurationService.subscribeToApplication(
                            subscriberId,
                            subscribeRequest.defaultApplicationName,
                            subscribeRequest.hostName,
                            subscribeRequest.applicationName,
                            subscribeRequest.lastKnownVersion,
                            subscriber
                        )
                        responseObserver.onNext(
                            toPropertiesChangesResponse(
                                subscribeRequest.applicationName,
                                changesToPush
                            )
                        )
                    }
                    else -> log.warn("Unrecognized message type = ${value.type}")
                }
            }

            override fun onError(t: Throwable?) {
                log.warn(
                    "Error in incoming stream, the bidirectional stream will be closed, unsubscribe will be called",
                    t
                )
                configurationService.unsubscribe(subscriberId)
            }

            override fun onCompleted() {
                configurationService.unsubscribe(subscriberId)
                responseObserver.onCompleted()
            }
        }
    }

    private fun requireRole(role: Role) {
        val userInfo = userInfo()
        if (!userInfo.hasRole(role)) {
            throw StatusRuntimeException(
                Status.UNAUTHENTICATED
                    .withDescription("Not enough permissions, user roles = ${userInfo.roles}")
            )
        }
    }
}
