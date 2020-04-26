package com.letsconfig.dashboard

import com.letsconfig.sdk.proto.ApplicationCreateRequest
import com.letsconfig.sdk.proto.ConfigurationServiceGrpc
import com.letsconfig.sdk.proto.EmptyRequest
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

    fun createApplication(appName: String) {
        val res = blockingClient.createApplication(ApplicationCreateRequest.newBuilder().setApplicationName(appName).build())
    }

    fun listApplications(): List<String> {
        val response = blockingClient.listApplications(EmptyRequest.getDefaultInstance())
        return response.applicationsList.map { it }
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