package com.configset.server.db.memory

import com.configset.sdk.ApplicationId
import com.configset.server.ApplicationED
import com.configset.server.CreateApplicationResul
import com.configset.server.DeleteApplicationResult
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.HostED
import com.configset.server.PropertyCreateResult
import com.configset.server.SearchPropertyRequest
import com.configset.server.UpdateApplicationResult
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.PropertyItemED
import com.configset.server.db.common.DbHandle
import com.configset.server.db.common.containsLowerCase
import java.util.concurrent.ThreadLocalRandom

class InMemoryConfigurationDao : ConfigurationDao {

    private var properties: MutableList<PropertyItemED> = mutableListOf()
    private val applications: MutableMap<ApplicationId, ApplicationED> = mutableMapOf()
    private val applicationsByName: MutableMap<String, ApplicationED> = mutableMapOf()
    private val hosts: MutableList<HostED> = mutableListOf()
    private var hostId = 0L
    private var appId = 0L

    @Synchronized
    override fun listApplications(): List<ApplicationED> {
        return applications.values.toList()
    }

    @Synchronized
    override fun createApplication(handle: DbHandle, appName: String): CreateApplicationResul {
        return processMutable {
            if (applicationsByName.containsKey(appName)) {
                return@processMutable CreateApplicationResul.ApplicationAlreadyExists
            }
            val ct = System.currentTimeMillis()
            val id = ApplicationId(appId++)
            val ed = ApplicationED(id, appName, 0L, ct, ct)
            applications[id] = ed
            applicationsByName[appName] = ed
            return@processMutable CreateApplicationResul.OK
        }
    }

    @Synchronized
    override fun deleteApplication(handle: DbHandle, applicationName: String): DeleteApplicationResult {
        val prev = applicationsByName.remove(applicationName)
        return if (prev == null) {
            DeleteApplicationResult.ApplicationNotFound
        } else {
            applications.remove(prev.id)
            properties = properties.filter { it.name != applicationName }.toMutableList()
            DeleteApplicationResult.OK
        }
    }

    @Synchronized
    override fun updateApplication(
        handle: DbHandle,
        id: ApplicationId,
        applicationName: String
    ): UpdateApplicationResult {
        val app = applications[id]
        return if (app == null) {
            UpdateApplicationResult.ApplicationNotFound
        } else {
            val prevName = app.name
            val newApp = app.copy(name = applicationName)
            applications[id] = newApp

            applicationsByName.remove(prevName)
            applicationsByName[applicationName] = newApp
            UpdateApplicationResult.OK
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

    override fun readProperty(applicationName: String, hostName: String, propertyName: String): PropertyItemED? {
        return properties.firstOrNull {
            it.applicationName == applicationName
                    && it.name == propertyName
                    && it.hostName == hostName
        }
    }

    @Synchronized
    override fun searchProperties(searchPropertyRequest: SearchPropertyRequest): List<PropertyItemED> {
        return properties.filter { !it.deleted }
            .mapNotNull { property ->
                if (searchPropertyRequest.applicationName != null
                    && property.applicationName != searchPropertyRequest.applicationName
                ) {
                    return@mapNotNull null
                }
                if (searchPropertyRequest.hostNameQuery != null && !containsLowerCase(
                        property.hostName,
                        searchPropertyRequest.hostNameQuery
                    )
                ) {
                    return@mapNotNull null
                }
                if (searchPropertyRequest.propertyNameQuery != null && !containsLowerCase(
                        property.name,
                        searchPropertyRequest.propertyNameQuery
                    )
                ) {
                    return@mapNotNull null
                }
                if (searchPropertyRequest.propertyValueQuery != null && !containsLowerCase(
                        property.value,
                        searchPropertyRequest.propertyValueQuery
                    )
                ) {
                    return@mapNotNull null
                }
                property
            }
    }

    @Synchronized
    override fun listProperties(applicationName: String): List<String> {
        return properties
            .filter { !it.deleted }
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
                if (!foundProperty.deleted && foundProperty.version != version) {
                    return@cb PropertyCreateResult.UpdateConflict
                } else {
                    properties.remove(foundProperty)
                }
            }
            val newVersion = lastVersion + 1
            val now = System.currentTimeMillis()
            properties.add(
                PropertyItemED(
                    id = foundProperty?.id ?: ThreadLocalRandom.current().nextLong(),
                    name = propertyName,
                    value = value,
                    hostName = hostName,
                    applicationName = appName,
                    version = newVersion,
                    deleted = false,
                    createdMs = foundProperty?.createdMs ?: now,
                    modifiedMs = foundProperty?.modifiedMs ?: now
                )
            )

            val app = applicationsByName[appName]!!
            val newApp = app.copy(lastVersion = newVersion)
            applicationsByName[appName] = newApp
            applications[newApp.id] = newApp
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
            properties.add(
                foundProperty.copy(
                    deleted = true,
                    version = newVersion,
                    modifiedMs = System.currentTimeMillis()
                )
            )

            val app = applicationsByName[appName]!!
            val newApp = app.copy(lastVersion = newVersion)
            applicationsByName[appName] = newApp
            applications[newApp.id] = newApp
            return@cb DeletePropertyResult.OK
        }
    }

    override fun initialize() {
    }

    private fun <T> processMutable(callback: () -> T): T {
        return callback.invoke()
    }

    private fun getLastVersionInApp(appName: String): Long? {
        return applicationsByName[appName]?.lastVersion
    }

    @Synchronized
    override fun getConfigurationSnapshotList(): List<PropertyItemED> {
        return properties.filter { applicationsByName.containsKey(it.applicationName) }
    }

    @Synchronized
    fun cleanup() {
        properties.clear()
        applications.clear()
        applicationsByName.clear()
        hosts.clear()
        hostId = 0L
        appId = 0L
    }
}
