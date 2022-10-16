package com.configset.server.db

import com.configset.server.ApplicationED
import com.configset.server.CreateApplicationResul
import com.configset.server.DeleteApplicationResult
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.HostED
import com.configset.server.PropertyCreateResult
import com.configset.server.SearchPropertyRequest
import com.configset.server.db.common.DbHandle

interface ConfigurationDao {
    fun initialize()
    fun listApplications(): List<ApplicationED>
    fun createApplication(handle: DbHandle, appName: String): CreateApplicationResul
    fun deleteApplication(handle: DbHandle, applicationName: String): DeleteApplicationResult
    fun createHost(handle: DbHandle, hostName: String): HostCreateResult
    fun listHosts(): List<HostED>
    fun updateProperty(
        handle: DbHandle,
        appName: String,
        propertyName: String,
        value: String,
        version: Long?,
        hostName: String,
    ): PropertyCreateResult

    fun deleteProperty(
        handle: DbHandle,
        appName: String,
        hostName: String,
        propertyName: String,
        version: Long,
    ): DeletePropertyResult

    fun getConfigurationSnapshotList(): List<PropertyItemED>
    fun searchProperties(searchPropertyRequest: SearchPropertyRequest): List<PropertyItemED>
    fun listProperties(applicationName: String): List<String>
    fun readProperty(applicationName: String, hostName: String, propertyName: String): PropertyItemED?
}

data class ConfigurationApplication(val appName: String, val config: Map<String, ConfigurationProperty>)

data class ConfigurationProperty(val propertyName: String, val hosts: Map<String, PropertyItemED>)

