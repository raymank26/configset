package com.configset.dashboard.property

import arrow.core.Either
import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.ServerApiGatewayErrorType
import com.configset.dashboard.util.RequestIdProducer
import com.configset.sdk.auth.UserInfo

class CrudPropertyService(
    private val serverApiGateway: ServerApiGateway,
    private val requestIdProducer: RequestIdProducer,
) {

    fun updateProperty(
        requestId: String,
        appName: String,
        hostName: String,
        propertyName: String,
        propertyValue: String,
        version: Long?,
        userInfo: UserInfo,
    ): Either<ServerApiGatewayErrorType, Unit> {
        // TODO: move logic below to config server
        if (version == null) {
            val hosts = serverApiGateway.listHosts(userInfo)
            if (!hosts.contains(hostName)) {
                val hostCreateResult = serverApiGateway.createHost(requestId, hostName, userInfo)
                if (hostCreateResult.isLeft()) {
                    return hostCreateResult
                }
            }
        }

        val updatePropertyRequestId = requestIdProducer.nextRequestId(requestId)
        return serverApiGateway.updateProperty(
            updatePropertyRequestId,
            appName,
            hostName,
            propertyName,
            propertyValue,
            version, userInfo
        )
    }

    fun deleteProperty(
        requestId: String,
        appName: String,
        hostName: String,
        propertyName: String,
        version: Long,
        userInfo: UserInfo,
    ): Either<ServerApiGatewayErrorType, Unit> {
        return serverApiGateway.deleteProperty(requestId, appName, hostName, propertyName, version, userInfo)
    }
}
