package com.configset.dashboard

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.configset.sdk.auth.UserInfo
import com.configset.sdk.client.ConfigSetClient
import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.sdk.proto.ApplicationCreatedResponse
import com.configset.sdk.proto.ApplicationDeleteRequest
import com.configset.sdk.proto.ApplicationDeletedResponse
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
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class ServerApiGateway(private val configSetClient: ConfigSetClient) {

    fun createApplication(
        requestId: String,
        appName: String,
        userInfo: UserInfo,
    ): Either<ServerApiGatewayErrorType, Unit> {
        val res = withClient(userInfo).createApplication(
            ApplicationCreateRequest.newBuilder()
                .setRequestId(requestId)
                .setApplicationName(appName)
                .build()
        )

        return when (res.type) {
            ApplicationCreatedResponse.Type.OK -> Unit.right()
            ApplicationCreatedResponse.Type.ALREADY_EXISTS -> ServerApiGatewayErrorType.CONFLICT.left()
            else -> throw RuntimeException("Unrecognized type for msg = $res")
        }
    }

    fun deleteApplication(applicationName: String, requestId: String, userInfo: UserInfo):
            Either<ServerApiGatewayErrorType, Unit> {
        val res = withClient(userInfo).deleteApplication(
            ApplicationDeleteRequest.newBuilder()
                .setRequestId(requestId)
                .setApplicationName(applicationName)
                .build()
        )

        return when (res.type) {
            ApplicationDeletedResponse.Type.OK -> Unit.right()
            ApplicationDeletedResponse.Type.APPLICATION_NOT_FOUND -> ServerApiGatewayErrorType.APPLICATION_NOT_FOUND
                .left()

            else -> throw RuntimeException("Unrecognized type for msg = $res")
        }
    }

    fun listApplications(userInfo: UserInfo): List<String> {
        val response = withClient(userInfo).listApplications(EmptyRequest.getDefaultInstance())
        return response.applicationsList.map { it }
    }

    fun listHosts(userInfo: UserInfo): List<String> {
        return withClient(userInfo).listHosts(EmptyRequest.getDefaultInstance())
            .hostNamesList
            .map { it }
    }

    fun searchProperties(
        searchPropertiesRequest: SearchPropertiesRequest,
        userInfo: UserInfo,
    ): List<ShowPropertyItem> {
        val response = withClient(userInfo).searchProperties(
            com.configset.sdk.proto.SearchPropertiesRequest
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
            ShowPropertyItem(
                id = item.id,
                applicationName = item.applicationName,
                hostName = item.hostName,
                propertyName = item.propertyName,
                propertyValue = item.propertyValue,
                version = item.version
            )
        }
    }

    fun listProperties(appName: String, userInfo: UserInfo): List<String> {
        return searchProperties(SearchPropertiesRequest(appName, null, null, null), userInfo)
            .mapNotNull {
                if (it.applicationName == appName) {
                    it.propertyName
                } else {
                    null
                }
            }
    }

    fun createHost(requestId: String, hostName: String, userInfo: UserInfo): Either<ServerApiGatewayErrorType, Unit> {
        val response = withClient(userInfo)
            .createHost(
                CreateHostRequest.newBuilder()
                    .setRequestId(requestId)
                    .setHostName(hostName)
                    .build()
            )
        return when (response.type) {
            CreateHostResponse.Type.OK -> Unit.right()
            CreateHostResponse.Type.HOST_ALREADY_EXISTS -> ServerApiGatewayErrorType.CONFLICT.left()
            else -> throw RuntimeException("Unrecognized type for msg = $response")
        }
    }

    fun readProperty(appName: String, hostName: String, propertyName: String, userInfo: UserInfo): PropertyItem? {
        val response = withClient(userInfo)
            .readProperty(
                ReadPropertyRequest.newBuilder()
                    .setApplicationName(appName)
                    .setHostName(hostName)
                    .setPropertyName(propertyName)
                    .build()
            )
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
        userInfo: UserInfo,
    ): Either<ServerApiGatewayErrorType, Unit> {
        val response = withClient(userInfo)
            .updateProperty(
                UpdatePropertyRequest.newBuilder()
                    .setRequestId(requestId)
                    .setApplicationName(appName)
                    .setHostName(hostName)
                    .setPropertyName(propertyName)
                    .setPropertyValue(propertyValue)
                    .setVersion(version ?: 0)
                    .build()
            )

        return when (response.type) {
            UpdatePropertyResponse.Type.OK ->
                Unit.right()

            UpdatePropertyResponse.Type.HOST_NOT_FOUND ->
                ServerApiGatewayErrorType.HOST_NOT_FOUND.left()

            UpdatePropertyResponse.Type.APPLICATION_NOT_FOUND ->
                ServerApiGatewayErrorType.APPLICATION_NOT_FOUND.left()

            UpdatePropertyResponse.Type.UPDATE_CONFLICT ->
                ServerApiGatewayErrorType.CONFLICT.left()

            else -> throw RuntimeException("Unrecognized type for msg = $response")
        }
    }

    fun deleteProperty(
        requestId: String,
        appName: String,
        hostName: String,
        propertyName: String,
        version: Long,
        userInfo: UserInfo,
    ): Either<ServerApiGatewayErrorType, Unit> {
        val response = withClient(userInfo).deleteProperty(
            DeletePropertyRequest.newBuilder()
                .setRequestId(requestId)
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .setVersion(version)
                .build()
        )
        return when (response.type) {
            DeletePropertyResponse.Type.OK -> Unit.right()
            DeletePropertyResponse.Type.PROPERTY_NOT_FOUND -> ServerApiGatewayErrorType.PROPERTY_NOT_FOUND.left()
            DeletePropertyResponse.Type.DELETE_CONFLICT -> ServerApiGatewayErrorType.CONFLICT.left()
            else -> throw RuntimeException("Unrecognized type for msg = $response")
        }
    }

    private fun withClient(userInfo: UserInfo): ConfigurationServiceGrpc.ConfigurationServiceBlockingStub {
        return configSetClient.getAuthBlockingClient(userInfo.accessToken)
    }
}

data class SearchPropertiesRequest(
    val applicationName: String?,
    val hostNameQuery: String?,
    val propertyNameQuery: String?,
    val propertyValueQuery: String?,
)

data class ShowPropertyItem @JsonCreator constructor(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("applicationName")
    val applicationName: String,
    @JsonProperty("hostName")
    val hostName: String,
    @JsonProperty("propertyName")
    val propertyName: String,
    @JsonProperty("propertyValue")
    val propertyValue: String,
    @JsonProperty("version")
    val version: Long,
)

data class TablePropertyItem @JsonCreator constructor(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("applicationName")
    val applicationName: String,
    @JsonProperty("propertyName")
    val propertyName: String,
    @JsonProperty("hostProperties")
    val hostProperties: List<ShowPropertyItem>,
)

