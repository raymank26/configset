package com.letsconfig

import com.letsconfig.db.ConfigurationDao

class InMemoryConfigurationDao(snapshot: List<PropertyItem>) : ConfigurationDao {

    private val properties: MutableList<PropertyItem> = snapshot.toMutableList()
    private val applications: MutableMap<String, Long>
    private val hosts: MutableList<String>

    init {
        applications = snapshot.groupBy { it.applicationName }
                .mapValues { properties.maxBy { it.version }!!.version }
                .toMutableMap()
        hosts = snapshot.map { it.hostName }.toMutableList()
    }

    override fun listApplications(): List<String> {
        return applications.keys.toList()
    }

    override fun createApplication(appName: String): CreateApplicationResult {
        if (applications.contains(appName)) {
            return CreateApplicationResult.ApplicationAlreadyExists
        } else {
            applications[appName] = 0
            return CreateApplicationResult.OK
        }
    }

    override fun createHost(hostName: String): HostCreateResult {
        if (hosts.contains(hostName)) {
            return HostCreateResult.HostAlreadyExists
        } else {
            hosts.add(hostName)
            return HostCreateResult.OK
        }
    }

    override fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult {
        val lastVersion = getLastVersionInApp(appName) ?: return PropertyCreateResult.ApplicationNotFound
        if (!hosts.contains(hostName)) {
            return PropertyCreateResult.HostNotFound
        }

        val foundProperty = properties.find { it.applicationName == appName && it.hostName == hostName && it.name == propertyName }
        if (foundProperty != null) {
            if (foundProperty.version != version) {
                return PropertyCreateResult.UpdateConflict
            } else {
                properties.remove(foundProperty)
            }
        }
        val newVersion = lastVersion + 1
        properties.add(PropertyItem.Updated(appName, propertyName, hostName, newVersion, value))
        applications[appName] = newVersion
        return PropertyCreateResult.OK
    }

    override fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult {
        val lastVersion = getLastVersionInApp(appName) ?: return DeletePropertyResult.PropertyNotFound
        if (!hosts.contains(hostName)) {
            return DeletePropertyResult.PropertyNotFound
        }
        val foundProperty = properties.find { it.applicationName == appName && it.hostName == hostName && it.name == propertyName }
                ?: return DeletePropertyResult.PropertyNotFound

        properties.remove(foundProperty)
        val newVersion = lastVersion + 1
        properties.add(PropertyItem.Deleted(appName, propertyName, hostName, lastVersion + 1))
        applications[appName] = newVersion
        return DeletePropertyResult.OK
    }

    private fun getLastVersionInApp(appName: String): Long? {
        return applications[appName]
    }

    override fun getConfigurationSnapshotList(): List<PropertyItem> {
        return properties
    }
}
