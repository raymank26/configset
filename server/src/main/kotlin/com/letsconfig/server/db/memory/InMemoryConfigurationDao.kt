package com.letsconfig.server.db.memory

import com.letsconfig.server.ApplicationED
import com.letsconfig.server.CreateApplicationResult
import com.letsconfig.server.DeletePropertyResult
import com.letsconfig.server.HostCreateResult
import com.letsconfig.server.HostED
import com.letsconfig.server.PropertyCreateResult
import com.letsconfig.server.PropertyItem
import com.letsconfig.server.SearchPropertyRequest
import com.letsconfig.server.db.ConfigurationDao
import com.letsconfig.server.db.common.PersistResult

class InMemoryConfigurationDao : ConfigurationDao {

    private val properties: MutableList<PropertyItem> = mutableListOf()
    private val applications: MutableList<ApplicationED> = mutableListOf()
    private val hosts: MutableList<HostED> = mutableListOf()
    private var hostId = 0L
    private var appId = 0L
    private var processedIds: MutableSet<String> = mutableSetOf()

    @Synchronized
    override fun listApplications(): List<ApplicationED> {
        return applications
    }

    @Synchronized
    override fun createApplication(requestId: String, appName: String): CreateApplicationResult {
        return processMutable<CreateApplicationResult>(requestId, CreateApplicationResult.OK) {
            if (applications.find { it.name == appName } != null) {
                PersistResult(false, CreateApplicationResult.ApplicationAlreadyExists)
            } else {
                val ct = System.currentTimeMillis()
                applications.add(ApplicationED(appId++, appName, 0L, ct, ct))
                PersistResult(true, CreateApplicationResult.OK)
            }
        }
    }

    @Synchronized
    override fun listHosts(): List<HostED> {
        return hosts
    }

    @Synchronized
    override fun createHost(requestId: String, hostName: String): HostCreateResult {
        return processMutable<HostCreateResult>(requestId, HostCreateResult.OK) {
            if (hosts.find { it.name == hostName } != null) {
                PersistResult(false, HostCreateResult.HostAlreadyExists)
            } else {
                val ct = System.currentTimeMillis()
                hosts.add(HostED(hostId++, hostName, ct, ct))
                PersistResult(true, HostCreateResult.OK)
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
            if (searchPropertyRequest.hostNameQuery != null && !property.hostName.contains(searchPropertyRequest.hostNameQuery)) {
                return@mapNotNull null
            }
            if (searchPropertyRequest.propertyNameQuery != null && !property.name.contains(searchPropertyRequest.propertyNameQuery)) {
                return@mapNotNull null
            }
            if (searchPropertyRequest.propertyValueQuery != null && !property.value.contains(searchPropertyRequest.propertyValueQuery)) {
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
    override fun updateProperty(requestId: String, appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult {
        return processMutable<PropertyCreateResult>(requestId, PropertyCreateResult.OK) cb@{
            val lastVersion = getLastVersionInApp(appName)
                    ?: return@cb PersistResult(false, PropertyCreateResult.ApplicationNotFound)
            if (hosts.find { it.name == hostName } == null) {
                return@cb PersistResult(false, PropertyCreateResult.HostNotFound)
            }

            val foundProperty = properties.find { it.applicationName == appName && it.hostName == hostName && it.name == propertyName }
            if (foundProperty != null) {
                if (foundProperty is PropertyItem.Updated && foundProperty.version != version) {
                    return@cb PersistResult(false, PropertyCreateResult.UpdateConflict)
                } else {
                    properties.remove(foundProperty)
                }
            }
            val newVersion = lastVersion + 1
            properties.add(PropertyItem.Updated(appName, propertyName, hostName, newVersion, value))

            val app = applications.find { it.name == appName }!!
            applications.remove(app)
            applications.add(app.copy(lastVersion = newVersion))
            return@cb PersistResult(true, PropertyCreateResult.OK)
        }
    }

    @Synchronized
    override fun deleteProperty(requestId: String, appName: String, hostName: String, propertyName: String, version: Long): DeletePropertyResult {
        return processMutable<DeletePropertyResult>(requestId, DeletePropertyResult.OK) cb@{
            val lastVersion = getLastVersionInApp(appName)
                    ?: return@cb PersistResult(false, DeletePropertyResult.PropertyNotFound)
            if (hosts.find { it.name == hostName } == null) {
                return@cb PersistResult(false, DeletePropertyResult.PropertyNotFound)
            }
            val foundProperty = properties.find { it.applicationName == appName && it.hostName == hostName && it.name == propertyName }
                    ?: return@cb PersistResult(false, DeletePropertyResult.PropertyNotFound)
            if (foundProperty.version != version) {
                return@cb PersistResult(false, DeletePropertyResult.DeleteConflict)
            }

            properties.remove(foundProperty)
            val newVersion = lastVersion + 1
            properties.add(PropertyItem.Deleted(appName, propertyName, hostName, newVersion))

            val app = applications.find { it.name == appName }!!
            applications.remove(app)
            applications.add(app.copy(lastVersion = newVersion))
            return@cb PersistResult(true, DeletePropertyResult.OK)
        }
    }

    private fun <T> processMutable(requestId: String, default: T, callback: () -> PersistResult<T>): T {
        if (processedIds.contains(requestId)) {
            return default
        }
        val res = callback.invoke()
        if (res.persistRequestId) {
            processedIds.add(requestId)
        }
        return res.res
    }

    private fun getLastVersionInApp(appName: String): Long? {
        return applications.find { it.name == appName }?.lastVersion
    }

    @Synchronized
    override fun getConfigurationSnapshotList(): List<PropertyItem> {
        return properties
    }
}
