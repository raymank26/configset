package com.configset.server.db

import com.configset.common.client.ApplicationId
import com.configset.server.ApplicationED
import com.configset.server.CreateApplicationResult
import com.configset.server.DeleteApplicationResult
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.HostED
import com.configset.server.PropertyCreateResult
import com.configset.server.SearchPropertyRequest
import com.configset.server.UpdateApplicationResult
import com.configset.server.db.common.DbHandle

interface ConfigurationDao {
    fun initialize()
    fun listApplications(handle: DbHandle): List<ApplicationED>
    fun createApplication(handle: DbHandle, appName: String): CreateApplicationResult
    fun deleteApplication(handle: DbHandle, applicationName: String): DeleteApplicationResult
    fun updateApplication(handle: DbHandle, id: ApplicationId, applicationName: String): UpdateApplicationResult
    fun createHost(handle: DbHandle, hostName: String): HostCreateResult
    fun listHosts(handle: DbHandle): List<HostED>
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

    fun getConfigurationSnapshotList(handle: DbHandle): List<PropertyItemED>
    fun searchProperties(handle: DbHandle, searchPropertyRequest: SearchPropertyRequest): List<PropertyItemED>
    fun listProperties(handle: DbHandle, applicationName: String): List<String>
    fun readProperty(handle: DbHandle, hostName: String, propertyName: String, applicationName: String): PropertyItemED?
}

data class ConfigurationApplication(val appName: String, val config: Map<String, ConfigurationProperty>)

data class ConfigurationProperty(val propertyName: String, val hosts: Map<String, PropertyItemED>)

