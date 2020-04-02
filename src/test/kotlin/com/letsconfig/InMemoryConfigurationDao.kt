package com.letsconfig

import com.letsconfig.db.ConfigurationApplication
import com.letsconfig.db.ConfigurationDao
import com.letsconfig.db.ConfigurationProperty

class InMemoryConfigurationDao(snapshot: Map<String, ConfigurationApplication>) : ConfigurationDao {

    private val inMemorySnapshot: MutableMap<String, InConfigurationApplication> = mutableMapOf()

    init {
        for ((appName, confApp) in snapshot) {
            val propConfig = mutableMapOf<String, InConfigurationProperty>()
            for ((propName, hostToPropItem) in confApp.config) {
                propConfig[propName] = InConfigurationProperty(propName, hostToPropItem.hosts.toMutableMap())
            }
            inMemorySnapshot[appName] = InConfigurationApplication(appName, propConfig)
        }
    }

    override fun listApplications(): List<String> {
        return inMemorySnapshot.keys.toList()
    }

    override fun createApplication(appName: String): CreateApplicationResult {
        var res: CreateApplicationResult = CreateApplicationResult.OK
        inMemorySnapshot.compute(appName) { _, value ->
            return@compute if (value == null) {
                InConfigurationApplication(appName, mutableMapOf())
            } else {
                res = CreateApplicationResult.ApplicationAlreadyExists
                value
            }
        }
        return res
    }

    override fun createHost(hostName: String): HostCreateResult {
        return HostCreateResult.OK
    }

    override fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult {
        val newVersion = version
                ?: getLastVersionInApp(appName)?.let { it + 1 }
                ?: return PropertyCreateResult.ApplicationNotFound
        return internalSet(PropertyItem.Updated(appName, propertyName, hostName, newVersion, value))
    }

    override fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult {
        val hostsMap = inMemorySnapshot[appName]?.config?.get(propertyName)?.hosts
        if (hostsMap == null || !hostsMap.contains(hostName)) {
            return DeletePropertyResult.PropertyNotFound
        }
        val lastVersionInApp = getLastVersionInApp(appName) ?: return DeletePropertyResult.PropertyNotFound
        internalSet(PropertyItem.Deleted(appName, propertyName, hostName, lastVersionInApp + 1))
        return DeletePropertyResult.OK
    }

    private fun internalSet(propertyItem: PropertyItem): PropertyCreateResult {
        var res: PropertyCreateResult = PropertyCreateResult.OK
        inMemorySnapshot.compute(propertyItem.applicationName) { _, content ->
            if (content == null) {
                res = PropertyCreateResult.ApplicationNotFound
                return@compute null
            }

            content.config.compute(propertyItem.name) { _, confProperty: InConfigurationProperty? ->
                if (confProperty == null) {
                    InConfigurationProperty(propertyItem.name, mutableMapOf(propertyItem.hostName to propertyItem))
                } else {
                    confProperty.hosts[propertyItem.hostName] = propertyItem
                    confProperty
                }
            }
            content
        }
        return res
    }

    private fun getLastVersionInApp(appName: String): Long? {
        return inMemorySnapshot[appName]?.config?.values?.flatMap { it.hosts.values.map { it.version } }?.max()
    }

    override fun getConfigurationSnapshot(): Map<String, ConfigurationApplication> {
        return inMemorySnapshot
    }
}

private data class InConfigurationApplication(
        override val appName: String,
        override val config: MutableMap<String, InConfigurationProperty>
) : ConfigurationApplication

private data class InConfigurationProperty(
        override val propertyName: String,
        override val hosts: MutableMap<String, PropertyItem>
) : ConfigurationProperty
