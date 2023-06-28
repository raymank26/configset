package com.configset.client

interface UpdatableConfiguration : Configuration {

    fun updateProperty(appName: String, name: String, value: String)

    fun deleteProperty(appName: String, name: String)
}
