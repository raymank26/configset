package com.letsconfig.dashboard

import com.letsconfig.sdk.proto.ApplicationCreateRequest
import com.letsconfig.sdk.proto.ApplicationCreatedResponse
import com.letsconfig.sdk.proto.ConfigurationServiceGrpc
import com.letsconfig.sdk.proto.CreateHostRequest
import com.letsconfig.sdk.proto.CreateHostResponse
import com.letsconfig.sdk.proto.EmptyRequest
import com.letsconfig.sdk.proto.ListPropertiesRequest
import com.letsconfig.sdk.proto.ListPropertiesResponse
import com.letsconfig.sdk.proto.UpdatePropertyRequest
import com.letsconfig.sdk.proto.UpdatePropertyResponse
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class ServerApiGateway(
        private val serverHostname: String,
        private val serverPort: Int) {

    private lateinit var blockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    private lateinit var channel: ManagedChannel

    fun start() {
        channel = ManagedChannelBuilder.forAddress(serverHostname, serverPort)
                .usePlaintext()
                .build()
        blockingClient = ConfigurationServiceGrpc.newBlockingStub(channel)
    }

    fun createApplication(requestId: String, appName: String): CreateApplicationResult {
        val res = blockingClient.createApplication(ApplicationCreateRequest.newBuilder()
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
        val response = blockingClient.listApplications(EmptyRequest.getDefaultInstance())
        return response.applicationsList.map { it }
    }

    fun listHosts(): List<String> {
        return blockingClient.listHosts(EmptyRequest.getDefaultInstance())
                .hostNamesList
                .map { it }
    }

    fun searchProperties(searchPropertiesRequest: SearchPropertiesRequest): Map<String, List<String>> {
        val response = blockingClient.searchProperties(com.letsconfig.sdk.proto.SearchPropertiesRequest
                .newBuilder()
                .setApplicationName(searchPropertiesRequest.applicationName)
                .setHostName(searchPropertiesRequest.hostNameQuery)
                .setPropertyName(searchPropertiesRequest.propertyNameQuery)
                .setPropertyName(searchPropertiesRequest.propertyValueQuery)
                .build())

        return response.itemsList.map { searchResponseItem ->
            Pair(searchResponseItem.appName, searchResponseItem.propertyNamesList.map { it })
        }.toMap()
    }
//
//    fun listProperties(listPropertiesRequest: ListPropertiesRequest) {
//
//    }

    fun createHost(requestId: String, hostName: String): CreateHostResult {
        val response = blockingClient.createHost(CreateHostRequest.newBuilder()
                .setRequestId(requestId)
                .setHostName(hostName)
                .build())
        return when (response.type) {
            CreateHostResponse.Type.OK -> CreateHostResult.OK
            CreateHostResponse.Type.HOST_ALREADY_EXISTS -> CreateHostResult.HostAlreadyExists
            else -> throw RuntimeException("Unrecognized type for msg = $response")
        }
    }

    fun updateProperty(requestId: String, appName: String, hostName: String, propertyName: String, propertyValue: String, version: Long?): PropertyCreateResult {
        val response = blockingClient.updateProperty(UpdatePropertyRequest.newBuilder()
                .setRequestId(requestId)
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .setPropertyValue(propertyValue)
                .setVersion(version ?: 0)
                .build())

        return when (response.type) {
            UpdatePropertyResponse.Type.OK -> PropertyCreateResult.OK
            UpdatePropertyResponse.Type.HOST_NOT_FOUND -> PropertyCreateResult.HostNotFound
            UpdatePropertyResponse.Type.APPLICATION_NOT_FOUND -> PropertyCreateResult.ApplicationNotFound
            UpdatePropertyResponse.Type.UPDATE_CONFLICT -> PropertyCreateResult.UpdateConflict
            else -> throw RuntimeException("Unrecognized type for msg = $response")
        }
    }

    fun stop() {
        channel.shutdown()
    }

    fun listProperties(appName: String): List<String> {
        val response: ListPropertiesResponse = blockingClient.listProperties(ListPropertiesRequest.newBuilder().setApplicationName(appName).build())
        return response.propertyNamesList.map { it }
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
    object HostNotFound : PropertyCreateResult()
    object ApplicationNotFound : PropertyCreateResult()
    object UpdateConflict : PropertyCreateResult()
}

