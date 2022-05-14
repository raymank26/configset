package com.configset.dashboard.property

import arrow.core.Either
import arrow.core.left
import com.configset.dashboard.ConfigurationUpdateError
import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.util.RequestIdProducer

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
        accessToken: String,
    ): Either<ConfigurationUpdateError, Unit> {
        // TODO: move logic below to config server
        val apps = serverApiGateway.listApplications(accessToken)
        if (!apps.contains(appName)) {
            return ConfigurationUpdateError.APPLICATION_NOT_FOUND.left()
        }
        if (version == null) {
            val hosts = serverApiGateway.listHosts(accessToken)
            if (!hosts.contains(hostName)) {
                serverApiGateway.createHost(requestId, hostName, accessToken)
            }
        }

        val updatePropertyRequestId = requestIdProducer.nextRequestId(requestId)
        return serverApiGateway.updateProperty(updatePropertyRequestId,
            appName,
            hostName,
            propertyName,
            propertyValue,
            version, accessToken)
    }

    fun deleteProperty(
        requestId: String,
        appName: String,
        hostName: String,
        propertyName: String,
        version: Long,
        accessToken: String,
    ): Either<ConfigurationUpdateError, Unit> {
        return serverApiGateway.deleteProperty(requestId, appName, hostName, propertyName, version, accessToken)
    }
}
