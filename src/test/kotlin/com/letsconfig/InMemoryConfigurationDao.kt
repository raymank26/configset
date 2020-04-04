package com.letsconfig

import com.letsconfig.db.ConfigurationDao

class InMemoryConfigurationDao : ConfigurationDao {

    private val properties: MutableList<PropertyItem> = mutableListOf()
    private val applications: MutableList<ApplicationED> = mutableListOf()
    private val hosts: MutableList<HostED> = mutableListOf()
    private var hostId = 0L
    private var appId = 0L

    override fun listApplications(): List<ApplicationED> {
        return applications
    }

    override fun createApplication(appName: String): CreateApplicationResult {
        if (applications.find { it.name == appName } != null) {
            return CreateApplicationResult.ApplicationAlreadyExists
        } else {
            val ct = System.currentTimeMillis()
            applications.add(ApplicationED(appId++, appName, 0L, ct, ct))
            return CreateApplicationResult.OK
        }
    }

    override fun listHosts(): List<HostED> {
        return hosts
    }

    override fun createHost(hostName: String): HostCreateResult {
        if (hosts.find { it.name == hostName } != null) {
            return HostCreateResult.HostAlreadyExists
        } else {
            val ct = System.currentTimeMillis()
            hosts.add(HostED(hostId++, hostName, ct, ct))
            return HostCreateResult.OK
        }
    }

    override fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult {
        val lastVersion = getLastVersionInApp(appName) ?: return PropertyCreateResult.ApplicationNotFound
        if (hosts.find { it.name == hostName } == null) {
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

        val app = applications.find { it.name == appName }!!
        applications.remove(app)
        applications.add(app.copy(lastVersion = newVersion))
        return PropertyCreateResult.OK
    }

    override fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult {
        val lastVersion = getLastVersionInApp(appName) ?: return DeletePropertyResult.PropertyNotFound
        if (hosts.find { it.name == hostName } == null) {
            return DeletePropertyResult.PropertyNotFound
        }
        val foundProperty = properties.find { it.applicationName == appName && it.hostName == hostName && it.name == propertyName }
                ?: return DeletePropertyResult.PropertyNotFound

        properties.remove(foundProperty)
        val newVersion = lastVersion + 1
        properties.add(PropertyItem.Deleted(appName, propertyName, hostName, newVersion))

        val app = applications.find { it.name == appName }!!
        applications.remove(app)
        applications.add(app.copy(lastVersion = newVersion))
        return DeletePropertyResult.OK
    }

    private fun getLastVersionInApp(appName: String): Long? {
        return applications.find { it.name == appName }?.lastVersion
    }

    override fun getConfigurationSnapshotList(): List<PropertyItem> {
        return properties
    }
}
