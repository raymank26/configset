package com.letsconfig.db

import com.letsconfig.CreateApplicationResult
import com.letsconfig.DeletePropertyResult
import com.letsconfig.HostCreateResult
import com.letsconfig.PropertyCreateResult
import com.letsconfig.PropertyItem

interface ConfigurationDao {
    fun listApplications(): List<String>
    fun createApplication(appName: String): CreateApplicationResult
    fun createHost(hostName: String): HostCreateResult
    fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult
    fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult
    fun getConfigurationSnapshot(): Map<String, ConfigurationApplication>
}

interface ConfigurationApplication {
    val appName: String
    val config: Map<String, ConfigurationProperty>
}

interface ConfigurationProperty {
    val propertyName: String
    val hosts: Map<String, PropertyItem>
}
