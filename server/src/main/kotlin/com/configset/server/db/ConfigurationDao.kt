package com.configset.server.db

import com.configset.server.ApplicationED
import com.configset.server.CreateApplicationResult
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.HostED
import com.configset.server.PropertyCreateResult
import com.configset.server.PropertyItem
import com.configset.server.SearchPropertyRequest

interface ConfigurationDao {
    fun initialize()
    fun listApplications(): List<ApplicationED>
    fun createApplication(requestId: String, appName: String): CreateApplicationResult
    fun createHost(requestId: String, hostName: String): HostCreateResult
    fun listHosts(): List<HostED>
    fun updateProperty(requestId: String, appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult
    fun deleteProperty(requestId: String, appName: String, hostName: String, propertyName: String, version: Long): DeletePropertyResult
    fun getConfigurationSnapshotList(): List<PropertyItem>
    fun searchProperties(searchPropertyRequest: SearchPropertyRequest): List<PropertyItem.Updated>
    fun listProperties(applicationName: String): List<String>
    fun readProperty(applicationName: String, hostName: String, propertyName: String): PropertyItem?
}

data class ConfigurationApplication(val appName: String, val config: Map<String, ConfigurationProperty>)

data class ConfigurationProperty(val propertyName: String, val hosts: Map<String, PropertyItem>)

