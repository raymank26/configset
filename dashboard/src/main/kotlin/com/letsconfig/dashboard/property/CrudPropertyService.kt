package com.letsconfig.dashboard.property

import com.letsconfig.dashboard.PropertyCreateResult
import com.letsconfig.dashboard.PropertyDeleteResult
import com.letsconfig.dashboard.ServerApiGateway
import com.letsconfig.dashboard.util.RequestIdProducer

class CrudPropertyService(
        private val serverApiGateway: ServerApiGateway,
        private val requestIdProducer: RequestIdProducer
) {

    fun updateProperty(requestId: String, appName: String, hostName: String, propertyName: String, propertyValue: String, version: Long?): PropertyCreateResult {
        val apps = serverApiGateway.listApplications()
        if (!apps.contains(appName)) {
            return PropertyCreateResult.ApplicationNotFound
        }
        if (version == null) {
            val hosts = serverApiGateway.listHosts()
            if (!hosts.contains(hostName)) {
                serverApiGateway.createHost(requestId, hostName)
            }
        }

        val updatePropertyRequestId = requestIdProducer.nextRequestId(requestId)
        return serverApiGateway.updateProperty(updatePropertyRequestId, appName, hostName, propertyName, propertyValue, version)
    }

    fun deleteProperty(requestId: String, appName: String, hostName: String, propertyName: String, version: Long): PropertyDeleteResult {
        return serverApiGateway.deleteProperty(requestId, appName, hostName, propertyName, version)
    }
}
