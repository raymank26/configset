package com.configset.server

import com.configset.server.db.ConfigurationDao
import com.configset.server.db.DbHandleFactory
import com.configset.server.db.RequestIdDao
import com.configset.server.db.common.DbHandle

class ConfigurationService(
    private val configurationDao: ConfigurationDao,
    private val requestIdDao: RequestIdDao,
    private val propertiesWatchDispatcher: PropertiesWatchDispatcher,
    private val dbHandleFactory: DbHandleFactory,
) {

    fun listApplications(): List<ApplicationED> {
        return configurationDao.listApplications()
    }

    fun createApplication(requestId: String, appName: String): CreateApplicationResult {
        checkRequestId(requestId)
        return executeMutable(requestId, CreateApplicationResult.OK) {
            configurationDao.createApplication(it, appName)
        }
    }

    fun createHost(requestId: String, hostName: String): HostCreateResult {
        checkRequestId(requestId)
        return executeMutable(requestId, HostCreateResult.OK) {
            configurationDao.createHost(it, hostName)
        }
    }

    fun listHosts(): List<HostED> {
        return configurationDao.listHosts()
    }

    fun updateProperty(requestId: String, appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult {
        checkRequestId(requestId)
        return executeMutable(requestId, PropertyCreateResult.OK) {
            configurationDao.updateProperty(it, appName, propertyName, value, version, hostName)
        }
    }

    fun deleteProperty(requestId: String, appName: String, hostName: String, propertyName: String, version: Long): DeletePropertyResult {
        checkRequestId(requestId)
        return executeMutable(requestId, DeletePropertyResult.OK) {
            configurationDao.deleteProperty(it, appName, hostName, propertyName, version)
        }
    }

    fun subscribeApplication(subscriberId: String, defaultApplicationName: String, hostName: String,
                             applicationName: String, lastKnownVersion: Long?): PropertiesChanges? {
        return propertiesWatchDispatcher.subscribeApplication(subscriberId, defaultApplicationName, hostName, applicationName, lastKnownVersion)
    }

    fun searchProperties(searchPropertyRequest: SearchPropertyRequest): List<PropertyItem.Updated> {
        return configurationDao.searchProperties(searchPropertyRequest)
    }

    fun listProperties(applicationName: String): List<String> {
        return configurationDao.listProperties(applicationName)
    }

    fun updateLastVersion(subscriberId: String, applicationName: String, version: Long) {
        propertiesWatchDispatcher.updateVersion(subscriberId, applicationName, version)
    }

    fun watchChanges(subscriber: WatchSubscriber) {
        propertiesWatchDispatcher.watchChanges(subscriber)
    }

    fun unsubscribe(subscriberId: String) {
        propertiesWatchDispatcher.unsubscribe(subscriberId)
    }

    private fun checkRequestId(requestId: String) {
        if (requestId.isEmpty()) {
            throw IllegalArgumentException("RequestId is empty")
        }
    }

    fun readProperty(applicationName: String, hostName: String, propertyName: String): PropertyItem? {
        return configurationDao.readProperty(applicationName, hostName, propertyName)
    }

    private fun <T> executeMutable(requestId: String, persistedValue: T, action: (DbHandle) -> T): T {
        return dbHandleFactory.withHandle {
            if (requestIdDao.exists(it, requestId)) {
                persistedValue
            } else {
                val res = action(it)
                if (res == persistedValue) {
                    requestIdDao.persist(it, requestId)
                }
                res
            }
        }
    }
}

interface WatchSubscriber {
    fun getId(): String
    fun pushChanges(applicationName: String, changes: PropertiesChanges)
}

sealed class PropertyItem {

    abstract val applicationName: String
    abstract val name: String
    abstract val version: Long
    abstract val hostName: String

    data class Updated(
            override val applicationName: String,
            override val name: String,
            override val hostName: String,
            override val version: Long,
            val value: String
    ) : PropertyItem()

    data class Deleted(
            override val applicationName: String,
            override val name: String,
            override val hostName: String,
            override val version: Long
    ) : PropertyItem()
}

sealed class CreateApplicationResult {
    object OK : CreateApplicationResult()
    object ApplicationAlreadyExists : CreateApplicationResult()
}

sealed class HostCreateResult {
    object OK : HostCreateResult()
    object HostAlreadyExists : HostCreateResult()
}

sealed class PropertyCreateResult {
    object OK : PropertyCreateResult()
    object HostNotFound : PropertyCreateResult()
    object ApplicationNotFound : PropertyCreateResult()
    object UpdateConflict : PropertyCreateResult()
}

sealed class DeletePropertyResult {
    object OK : DeletePropertyResult()
    object DeleteConflict : DeletePropertyResult()
    object PropertyNotFound : DeletePropertyResult()
}

data class ApplicationED(val id: Long?, val name: String, val lastVersion: Long, val createdMs: Long, val modifiedMs: Long)

data class HostED(val id: Long?, val name: String, val createdMs: Long, val modifiedMs: Long)

data class SearchPropertyRequest(val applicationName: String?, val propertyNameQuery: String?, val propertyValueQuery: String?, val hostNameQuery: String?)

data class TableMetaED(val version: Long)
