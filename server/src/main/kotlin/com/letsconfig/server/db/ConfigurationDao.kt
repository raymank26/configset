package com.letsconfig.server.db

import com.letsconfig.server.ApplicationED
import com.letsconfig.server.CreateApplicationResult
import com.letsconfig.server.DeletePropertyResult
import com.letsconfig.server.HostCreateResult
import com.letsconfig.server.HostED
import com.letsconfig.server.PropertyCreateResult
import com.letsconfig.server.PropertyItem
import com.letsconfig.server.SearchPropertyRequest

interface ConfigurationDao {
    fun listApplications(): List<ApplicationED>
    fun createApplication(requestId: String, appName: String): CreateApplicationResult
    fun createHost(requestId: String, hostName: String): HostCreateResult
    fun listHosts(): List<HostED>
    fun updateProperty(requestId: String, appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult
    fun deleteProperty(requestId: String, appName: String, hostName: String, propertyName: String): DeletePropertyResult
    fun getConfigurationSnapshotList(): List<PropertyItem>
    fun searchProperties(searchPropertyRequest: SearchPropertyRequest): Map<String, List<String>>
    fun listProperties(applicationName: String): List<String>
}

data class ConfigurationApplication(val appName: String, val config: Map<String, ConfigurationProperty>)

data class ConfigurationProperty(val propertyName: String, val hosts: Map<String, PropertyItem>)

