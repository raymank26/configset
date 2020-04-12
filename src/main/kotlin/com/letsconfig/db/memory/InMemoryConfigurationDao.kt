package com.letsconfig.db.memory

import com.letsconfig.ApplicationED
import com.letsconfig.CreateApplicationResult
import com.letsconfig.DeletePropertyResult
import com.letsconfig.HostCreateResult
import com.letsconfig.HostED
import com.letsconfig.PropertyCreateResult
import com.letsconfig.PropertyItem
import com.letsconfig.db.ConfigurationDao
import com.letsconfig.db.common.PersistResult

class InMemoryConfigurationDao : ConfigurationDao {

    private val properties: MutableList<PropertyItem> = mutableListOf()
    private val applications: MutableList<ApplicationED> = mutableListOf()
    private val hosts: MutableList<HostED> = mutableListOf()
    private var hostId = 0L
    private var appId = 0L
    private var processedIds: MutableSet<String> = mutableSetOf()

    override fun listApplications(): List<ApplicationED> {
        return applications
    }

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

    override fun listHosts(): List<HostED> {
        return hosts
    }

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

    override fun updateProperty(requestId: String, appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult {
        return processMutable<PropertyCreateResult>(requestId, PropertyCreateResult.OK) cb@{
            val lastVersion = getLastVersionInApp(appName)
                    ?: return@cb PersistResult(false, PropertyCreateResult.ApplicationNotFound)
            if (hosts.find { it.name == hostName } == null) {
                return@cb PersistResult(false, PropertyCreateResult.HostNotFound)
            }

            val foundProperty = properties.find { it.applicationName == appName && it.hostName == hostName && it.name == propertyName }
            if (foundProperty != null) {
                if (foundProperty.version != version) {
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

    override fun deleteProperty(requestId: String, appName: String, hostName: String, propertyName: String): DeletePropertyResult {
        return processMutable<DeletePropertyResult>(requestId, DeletePropertyResult.OK) cb@{
            val lastVersion = getLastVersionInApp(appName)
                    ?: return@cb PersistResult(false, DeletePropertyResult.PropertyNotFound)
            if (hosts.find { it.name == hostName } == null) {
                return@cb PersistResult(false, DeletePropertyResult.PropertyNotFound)
            }
            val foundProperty = properties.find { it.applicationName == appName && it.hostName == hostName && it.name == propertyName }
                    ?: return@cb PersistResult(false, DeletePropertyResult.PropertyNotFound)

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

    override fun getConfigurationSnapshotList(): List<PropertyItem> {
        return properties
    }
}
