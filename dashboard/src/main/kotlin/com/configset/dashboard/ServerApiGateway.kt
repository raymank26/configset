package com.configset.dashboard

import com.configset.sdk.client.ConfigSetClient
import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.sdk.proto.ApplicationCreatedResponse
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.sdk.proto.CreateHostRequest
import com.configset.sdk.proto.CreateHostResponse
import com.configset.sdk.proto.DeletePropertyRequest
import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.EmptyRequest
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.ReadPropertyRequest
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse

class ServerApiGateway(
    private val serverHostname: String,
    private val serverPort: Int,
) {

    private lateinit var configSetClient: ConfigSetClient

    fun start() {
        configSetClient = ConfigSetClient(serverHostname, serverPort)
    }

    fun createApplication(requestId: String, appName: String, accessToken: String): CreateApplicationResult {
        val res = withClient(accessToken).createApplication(ApplicationCreateRequest.newBuilder()
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

    fun listApplications(accessToken: String): List<String> {
        val response = withClient(accessToken).listApplications(EmptyRequest.getDefaultInstance())
        return response.applicationsList.map { it }
    }

    fun listHosts(accessToken: String): List<String> {
        return withClient(accessToken).listHosts(EmptyRequest.getDefaultInstance())
            .hostNamesList
            .map { it }
    }

    fun searchProperties(
        searchPropertiesRequest: SearchPropertiesRequest,
        accessToken: String,
    ): List<ShowPropertyItem> {
        val response = withClient(accessToken).searchProperties(com.configset.sdk.proto.SearchPropertiesRequest
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

    fun listProperties(appName: String, accessToken: String): List<String> {
        return searchProperties(SearchPropertiesRequest(appName, null, null, null), accessToken)
            .mapNotNull {
                if (it.applicationName == appName) {
                    it.propertyName
                } else {
                    null
                }
            }
    }

    fun createHost(requestId: String, hostName: String, accessToken: String): CreateHostResult {
        val response = withClient(accessToken).createHost(CreateHostRequest.newBuilder()
            .setRequestId(requestId)
            .setHostName(hostName)
            .build())
        return when (response.type) {
            CreateHostResponse.Type.OK -> CreateHostResult.OK
            CreateHostResponse.Type.HOST_ALREADY_EXISTS -> CreateHostResult.HostAlreadyExists
            else -> throw RuntimeException("Unrecognized type for msg = $response")
        }
    }

    fun readProperty(appName: String, hostName: String, propertyName: String, accessToken: String): PropertyItem? {
        val response = withClient(accessToken).readProperty(ReadPropertyRequest.newBuilder()
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

    fun updateProperty(
        requestId: String,
        appName: String,
        hostName: String,
        propertyName: String,
        propertyValue: String,
        version: Long?,
        accessToken: String,
    ): PropertyCreateResult {
        val response = withClient(accessToken).updateProperty(UpdatePropertyRequest.newBuilder()
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

    fun deleteProperty(
        requestId: String,
        appName: String,
        hostName: String,
        propertyName: String,
        version: Long,
        accessToken: String,
    ): PropertyDeleteResult {
        val response = withClient(accessToken).deleteProperty(DeletePropertyRequest.newBuilder()
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

    private fun withClient(accessToken: String): ConfigurationServiceGrpc.ConfigurationServiceBlockingStub {
        return configSetClient.getAuthBlockingClient(accessToken)
    }

    fun stop() {
        configSetClient.stop()
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

