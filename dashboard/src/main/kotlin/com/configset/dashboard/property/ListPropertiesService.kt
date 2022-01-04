package com.configset.dashboard.property

import com.configset.dashboard.SearchPropertiesRequest
import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.ShowPropertyItem

class ListPropertiesService(
        private val apiGateway: ServerApiGateway
) {
    fun list(appName: String, accessToken: String): List<String> {
        return apiGateway.listProperties(appName, accessToken)
    }

    fun searchProperties(
        searchPropertiesRequest: SearchPropertiesRequest,
        accessToken: String,
    ): List<ShowPropertyItem> {
        return apiGateway.searchProperties(searchPropertiesRequest, accessToken)
    }

    fun getProperty(appName: String, hostName: String, propertyName: String, accessToken: String): ShowPropertyItem? {
        return apiGateway.readProperty(appName, hostName, propertyName, accessToken)?.let {
            ShowPropertyItem(appName, hostName, propertyName, it.propertyValue, it.version)
        }
    }
}