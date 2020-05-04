package com.letsconfig.dashboard.property

import com.letsconfig.dashboard.ServerApiGateway

class ListPropertiesService(
        private val apiGateway: ServerApiGateway
) {
    fun list(appName: String?, hostName: String?, propertyName: String?, propertyValue: String?) {
        if (appName != null && hostName == null && propertyName == null && propertyValue == null) {
//            apiGateway.searchProperties()
        }
    }
}