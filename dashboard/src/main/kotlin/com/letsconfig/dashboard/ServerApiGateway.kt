package com.letsconfig.dashboard

import com.letsconfig.sdk.proto.ApplicationCreateRequest
import com.letsconfig.sdk.proto.ApplicationCreatedResponse
import com.letsconfig.sdk.proto.ConfigurationServiceGrpc
import com.letsconfig.sdk.proto.CreateHostRequest
import com.letsconfig.sdk.proto.CreateHostResponse
import com.letsconfig.sdk.proto.DeletePropertyRequest
import com.letsconfig.sdk.proto.DeletePropertyResponse
import com.letsconfig.sdk.proto.EmptyRequest
import com.letsconfig.sdk.proto.PropertyItem
import com.letsconfig.sdk.proto.ReadPropertyRequest
import com.letsconfig.sdk.proto.UpdatePropertyRequest
import com.letsconfig.sdk.proto.UpdatePropertyResponse
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

class ServerApiGateway(
        private val serverHostname: String,
        private val serverPort: Int,
        private val networkTimeout: Long) {

    private lateinit var blockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    private lateinit var channel: ManagedChannel

    fun start() {
        channel = ManagedChannelBuilder.forAddress(serverHostname, serverPort)
                .usePlaintext()
                .keepAliveTime(5000, TimeUnit.MILLISECONDS)
                .build()
        blockingClient = ConfigurationServiceGrpc.newBlockingStub(channel)
    }

    fun createApplication(requestId: String, appName: String): CreateApplicationResult {
        val res = withDeadline().createApplication(ApplicationCreateRequest.newBuilder()
                .setRequestId(requestId)
                .setApplicationName(appName)
                .build()
        )

        return when (res.type) {
            ApplicationCreatedResponse.Type.OK -> CreateApplicationResult.OK
            ApplicationCreatedResponse.Type.ALREADY_EXISTS -> CreateApplicationResult.ApplicationAlreadyExists
            else -> throw RuntimeException("Unrecognized type for msg = $res")
        }
    }

    fun listApplications(): List<String> {
        val response = withDeadline().listApplications(EmptyRequest.getDefaultInstance())
        return response.applicationsList.map { it }
    }

    fun listHosts(): List<String> {
        return withDeadline().listHosts(EmptyRequest.getDefaultInstance())
                .hostNamesList
                .map { it }
    }

    fun searchProperties(searchPropertiesRequest: SearchPropertiesRequest): List<ShowPropertyItem> {
        val response = withDeadline().searchProperties(com.letsconfig.sdk.proto.SearchPropertiesRequest
                .newBuilder()
                .apply {
                    if (searchPropertiesRequest.applicationName != null) {
                        applicationName = searchPropertiesRequest.applicationName
                    }
                    if (searchPropertiesRequest.hostNameQuery != null) {
                        hostName = searchPropertiesRequest.hostNameQuery
                    }
                    if (searchPropertiesRequest.propertyNameQuery != null) {
                        propertyName = searchPropertiesRequest.propertyNameQuery
                    }
                    if (searchPropertiesRequest.propertyValueQuery != null) {
                        propertyValue = searchPropertiesRequest.propertyValueQuery
                    }
                }
                .build())

        return response.itemsList.map { item ->
            ShowPropertyItem(item.applicationName, item.hostName, item.propertyName, item.propertyValue, item.version)
        }
    }

    fun listProperties(appName: String): List<String> {
        return searchProperties(SearchPropertiesRequest(appName, null, null, null))
                .mapNotNull {
                    if (it.applicationName == appName) {
                        it.propertyName
                    } else {
                        null
                    }
                }
    }

    fun createHost(requestId: String, hostName: String): CreateHostResult {
        val response = withDeadline().createHost(CreateHostRequest.newBuilder()
                .setRequestId(requestId)
                .setHostName(hostName)
                .build())
        return when (response.type) {
            CreateHostResponse.Type.OK -> CreateHostResult.OK
            CreateHostResponse.Type.HOST_ALREADY_EXISTS -> CreateHostResult.HostAlreadyExists
            else -> throw RuntimeException("Unrecognized type for msg = $response")
        }
    }

    fun readProperty(appName: String, hostName: String, propertyName: String): PropertyItem? {
        val response = withDeadline().readProperty(ReadPropertyRequest.newBuilder()
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .build())
        return if (response.hasItem) {
            response.item
        } else {
            null
        }
    }

    fun updateProperty(requestId: String, appName: String, hostName: String, propertyName: String, propertyValue: String, version: Long?): PropertyCreateResult {
        val response = withDeadline().updateProperty(UpdatePropertyRequest.newBuilder()
                .setRequestId(requestId)
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .setPropertyValue(propertyValue)
                .setVersion(version ?: 0)
                .build())

        return when (response.type) {
            UpdatePropertyResponse.Type.OK -> PropertyCreateResult.OK
            UpdatePropertyResponse.Type.HOST_NOT_FOUND -> throw java.lang.RuntimeException("Host not found but has to be created")
            UpdatePropertyResponse.Type.APPLICATION_NOT_FOUND -> PropertyCreateResult.ApplicationNotFound
            UpdatePropertyResponse.Type.UPDATE_CONFLICT -> PropertyCreateResult.UpdateConflict
            else -> throw RuntimeException("Unrecognized type for msg = $response")
        }
    }

    fun deleteProperty(requestId: String, appName: String, hostName: String, propertyName: String, version: Long): PropertyDeleteResult {
        val response = withDeadline().deleteProperty(DeletePropertyRequest.newBuilder()
                .setRequestId(requestId)
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .setVersion(version)
                .build()
        )
        return when (response.type) {
            DeletePropertyResponse.Type.OK -> PropertyDeleteResult.OK
            DeletePropertyResponse.Type.PROPERTY_NOT_FOUND -> PropertyDeleteResult.OK
            DeletePropertyResponse.Type.DELETE_CONFLICT -> PropertyDeleteResult.DeleteConflict
            else -> throw RuntimeException("Unrecognized type for msg = $response")
        }
    }

    private fun withDeadline(): ConfigurationServiceGrpc.ConfigurationServiceBlockingStub {
        return blockingClient.withDeadlineAfter(networkTimeout, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        channel.shutdown()
    }
}

data class SearchPropertiesRequest(
        val applicationName: String?,
        val hostNameQuery: String?,
        val propertyNameQuery: String?,
        val propertyValueQuery: String?
)

sealed class CreateApplicationResult {
    object OK : CreateApplicationResult()
    object ApplicationAlreadyExists : CreateApplicationResult()
}

sealed class CreateHostResult {
    object OK : CreateHostResult()
    object HostAlreadyExists : CreateHostResult()
}

sealed class PropertyCreateResult {
    object OK : PropertyCreateResult()
    object ApplicationNotFound : PropertyCreateResult()
    object UpdateConflict : PropertyCreateResult()
}

sealed class PropertyDeleteResult {
    object OK : PropertyDeleteResult()
    object DeleteConflict : PropertyDeleteResult()
}

data class ShowPropertyItem(val applicationName: String, val hostName: String, val propertyName: String,
                            val propertyValue: String, val version: Long)

