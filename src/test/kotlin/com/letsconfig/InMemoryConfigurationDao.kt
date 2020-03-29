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

    override fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: Long): PropertyCreateResult {
        var res: PropertyCreateResult = PropertyCreateResult.OK
        inMemorySnapshot.compute(appName) { _, content ->
            if (content == null) {
                res = PropertyCreateResult.ApplicationNotFound
                return@compute null
            }
            val propertyItem = PropertyItem.Updated(appName, propertyName, hostName, version, value)

            val confProperty: InConfigurationProperty? = content.config[propertyName]
            if (confProperty == null) {
                InConfigurationProperty(propertyName, mutableMapOf(hostName to propertyItem))
            } else {
                confProperty.hosts[hostName] = propertyItem
            }
            content
        }
        return res
    }

    override fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult {
        TODO()
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
