package com.letsconfig.dashboard.property

import com.letsconfig.dashboard.ServerApiGateway

class ListPropertiesService(
        private val apiGateway: ServerApiGateway
) {
    fun list(appName: String): List<String> {
        return apiGateway.listProperties(appName)
    }
}