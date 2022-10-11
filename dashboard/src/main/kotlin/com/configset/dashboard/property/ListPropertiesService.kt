package com.configset.dashboard.property

import com.configset.dashboard.SearchPropertiesRequest
import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.ShowPropertyItem
import com.configset.dashboard.TablePropertyItem
import org.apache.commons.lang3.RandomStringUtils

class ListPropertiesService(
        private val apiGateway: ServerApiGateway
) {
    fun list(appName: String, accessToken: String): List<String> {
        return apiGateway.listProperties(appName, accessToken)
    }

    fun searchProperties(
        searchPropertiesRequest: SearchPropertiesRequest,
        accessToken: String,
    ): List<TablePropertyItem> {
        return apiGateway.searchProperties(searchPropertiesRequest, accessToken)
            .groupBy { it.applicationName to it.propertyName }
            .map { TablePropertyItem(RandomStringUtils.randomAlphabetic(10), it.key.first, it.key.second, it.value) }
    }

    fun getProperty(appName: String, hostName: String, propertyName: String, accessToken: String): ShowPropertyItem? {
        return apiGateway.readProperty(appName, hostName, propertyName, accessToken)?.let {
            ShowPropertyItem(it.id, appName, hostName, propertyName, it.propertyValue, it.version)
        }
    }
}