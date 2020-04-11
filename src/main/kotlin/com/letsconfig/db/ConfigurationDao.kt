package com.letsconfig.db

import com.letsconfig.ApplicationED
import com.letsconfig.CreateApplicationResult
import com.letsconfig.DeletePropertyResult
import com.letsconfig.HostCreateResult
import com.letsconfig.HostED
import com.letsconfig.PropertyCreateResult
import com.letsconfig.PropertyItem

interface ConfigurationDao {
    fun listApplications(): List<ApplicationED>
    fun createApplication(appName: String): CreateApplicationResult
    fun createHost(hostName: String): HostCreateResult
    fun listHosts(): List<HostED>
    fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult
    fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult
    fun getConfigurationSnapshotList(): List<PropertyItem>
}

data class ConfigurationApplication(val appName: String, val config: Map<String, ConfigurationProperty>)

data class ConfigurationProperty(val propertyName: String, val hosts: Map<String, PropertyItem>)
