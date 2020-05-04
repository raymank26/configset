package com.letsconfig.dashboard.property

import com.letsconfig.dashboard.SearchPropertiesRequest
import com.letsconfig.dashboard.ServerApiGateway

class ListPropertiesService(
        private val apiGateway: ServerApiGateway
) {
    fun list(appName: String): List<String> {
        return apiGateway.listProperties(appName)
    }

    fun searchProperties(searchPropertiesRequest: SearchPropertiesRequest): Map<String, List<String>> {
        return apiGateway.searchProperties(searchPropertiesRequest)
    }
}