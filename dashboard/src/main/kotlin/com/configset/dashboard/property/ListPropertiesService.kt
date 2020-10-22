package com.configset.dashboard.property

import com.configset.dashboard.SearchPropertiesRequest
import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.ShowPropertyItem

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