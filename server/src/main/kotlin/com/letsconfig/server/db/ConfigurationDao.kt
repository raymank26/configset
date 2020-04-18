package com.letsconfig.server.db

import com.letsconfig.server.ApplicationED
import com.letsconfig.server.CreateApplicationResult
import com.letsconfig.server.DeletePropertyResult
import com.letsconfig.server.HostCreateResult
import com.letsconfig.server.HostED
import com.letsconfig.server.PropertyCreateResult
import com.letsconfig.server.PropertyItem

interface ConfigurationDao {
    fun listApplications(): List<ApplicationED>
    fun createApplication(requestId: String, appName: String): CreateApplicationResult
    fun createHost(requestId: String, hostName: String): HostCreateResult
    fun listHosts(): List<HostED>
    fun updateProperty(requestId: String, appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult
    fun deleteProperty(requestId: String, appName: String, hostName: String, propertyName: String): DeletePropertyResult
    fun getConfigurationSnapshotList(): List<PropertyItem>
}

data class ConfigurationApplication(val appName: String, val config: Map<String, ConfigurationProperty>)

data class ConfigurationProperty(val propertyName: String, val hosts: Map<String, PropertyItem>)

