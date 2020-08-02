package com.letsconfig.dashboard.property

import com.letsconfig.dashboard.SearchPropertiesRequest
import com.letsconfig.dashboard.ServerApiGateway
import com.letsconfig.dashboard.ShowPropertyItem

class ListPropertiesService(
        private val apiGateway: ServerApiGateway
) {
    fun list(appName: String): List<String> {
        return apiGateway.listProperties(appName)
    }

    fun searchProperties(searchPropertiesRequest: SearchPropertiesRequest): List<ShowPropertyItem> {
        return apiGateway.searchProperties(searchPropertiesRequest)
    }

    fun getProperty(appName: String, hostName: String, propertyName: String): ShowPropertyItem? {
        return apiGateway.readProperty(appName, hostName, propertyName)?.let {
            ShowPropertyItem(appName, hostName, propertyName, it.propertyValue, it.version)
        }
    }
}