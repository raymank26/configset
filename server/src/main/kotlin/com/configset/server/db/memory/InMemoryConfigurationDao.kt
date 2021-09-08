package com.configset.server.db.memory

import com.configset.server.ApplicationED
import com.configset.server.CreateApplicationResult
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.HostED
import com.configset.server.PropertyCreateResult
import com.configset.server.PropertyItem
import com.configset.server.SearchPropertyRequest
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.common.DbHandle
import com.configset.server.db.common.containsLowerCase

class InMemoryConfigurationDao : ConfigurationDao {

    private val properties: MutableList<PropertyItem> = mutableListOf()
    private val applications: MutableList<ApplicationED> = mutableListOf()
    private val hosts: MutableList<HostED> = mutableListOf()
    private var hostId = 0L
    private var appId = 0L

    @Synchronized
    override fun listApplications(): List<ApplicationED> {
        return applications
    }

    @Synchronized
    override fun createApplication(handle: DbHandle, appName: String): CreateApplicationResult {
        return processMutable {
            if (applications.find { it.name == appName } != null) {
                CreateApplicationResult.ApplicationAlreadyExists
            } else {
                val ct = System.currentTimeMillis()
                applications.add(ApplicationED(appId++, appName, 0L, ct, ct))
                CreateApplicationResult.OK
            }
        }
    }

    @Synchronized
    override fun listHosts(): List<HostED> {
        return hosts
    }

    @Synchronized
    override fun createHost(handle: DbHandle, hostName: String): HostCreateResult {
        return processMutable {
            if (hosts.find { it.name == hostName } != null) {
                HostCreateResult.HostAlreadyExists
            } else {
                val ct = System.currentTimeMillis()
                hosts.add(HostED(hostId++, hostName, ct, ct))
                HostCreateResult.OK
            }
        }
    }

    override fun readProperty(applicationName: String, hostName: String, propertyName: String): PropertyItem? {
        return properties.firstOrNull { it.applicationName == applicationName && it.name == propertyName && it.hostName == hostName }
    }

    @Synchronized
    override fun searchProperties(searchPropertyRequest: SearchPropertyRequest): List<PropertyItem.Updated> {
        return properties.filterIsInstance(PropertyItem.Updated::class.java).mapNotNull { property ->
            if (searchPropertyRequest.applicationName != null && property.applicationName != searchPropertyRequest.applicationName) {
                return@mapNotNull null
            }
            if (searchPropertyRequest.hostNameQuery != null && !containsLowerCase(property.hostName, searchPropertyRequest.hostNameQuery)) {
                return@mapNotNull null
            }
            if (searchPropertyRequest.propertyNameQuery != null && !containsLowerCase(property.name, searchPropertyRequest.propertyNameQuery)) {
                return@mapNotNull null
            }
            if (searchPropertyRequest.propertyValueQuery != null && !containsLowerCase(property.value, searchPropertyRequest.propertyValueQuery)) {
                return@mapNotNull null
            }
            property
        }
    }

    @Synchronized
    override fun listProperties(applicationName: String): List<String> {
        return properties
                .filterIsInstance(PropertyItem.Updated::class.java)
                .filter { it.applicationName == applicationName }
                .map { it.name }.distinct()
    }

    @Synchronized
    override fun updateProperty(
        handle: DbHandle,
        appName: String,
        propertyName: String,
        value: String,
        version: Long?,
        hostName: String,
    ): PropertyCreateResult {
        return processMutable cb@{
            val lastVersion = getLastVersionInApp(appName)
                ?: return@cb PropertyCreateResult.ApplicationNotFound
            if (hosts.find { it.name == hostName } == null) {
                return@cb PropertyCreateResult.HostNotFound
            }

            val foundProperty =
                properties.find { it.applicationName == appName && it.hostName == hostName && it.name == propertyName }
            if (foundProperty != null) {
                if (foundProperty is PropertyItem.Updated && foundProperty.version != version) {
                    return@cb PropertyCreateResult.UpdateConflict
                } else {
                    properties.remove(foundProperty)
                }
            }
            val newVersion = lastVersion + 1
            properties.add(PropertyItem.Updated(appName, propertyName, hostName, newVersion, value))

            val app = applications.find { it.name == appName }!!
            applications.remove(app)
            applications.add(app.copy(lastVersion = newVersion))
            return@cb PropertyCreateResult.OK
        }
    }

    @Synchronized
    override fun deleteProperty(
        handle: DbHandle,
        appName: String,
        hostName: String,
        propertyName: String,
        version: Long,
    ): DeletePropertyResult {
        return processMutable cb@{
            val lastVersion = getLastVersionInApp(appName)
                ?: return@cb DeletePropertyResult.PropertyNotFound
            if (hosts.find { it.name == hostName } == null) {
                return@cb DeletePropertyResult.PropertyNotFound
            }
            val foundProperty =
                properties.find { it.applicationName == appName && it.hostName == hostName && it.name == propertyName }
                    ?: return@cb DeletePropertyResult.PropertyNotFound
            if (foundProperty.version != version) {
                return@cb DeletePropertyResult.DeleteConflict
            }

            properties.remove(foundProperty)
            val newVersion = lastVersion + 1
            properties.add(PropertyItem.Deleted(appName, propertyName, hostName, newVersion))

            val app = applications.find { it.name == appName }!!
            applications.remove(app)
            applications.add(app.copy(lastVersion = newVersion))
            return@cb DeletePropertyResult.OK
        }
    }

    override fun initialize() {
    }

    private fun <T> processMutable(callback: () -> T): T {
        return callback.invoke()
    }

    private fun getLastVersionInApp(appName: String): Long? {
        return applications.find { it.name == appName }?.lastVersion
    }

    @Synchronized
    override fun getConfigurationSnapshotList(): List<PropertyItem> {
        return properties
    }
}