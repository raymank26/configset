package com.configset.server

import com.configset.sdk.ApplicationId
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.DbHandleFactory
import com.configset.server.db.PropertyItemED
import com.configset.server.db.RequestIdDao
import com.configset.server.db.common.DbHandle

class ConfigurationService(
    private val configurationDao: ConfigurationDao,
    private val requestIdDao: RequestIdDao,
    private val propertiesWatchDispatcher: PropertiesWatchDispatcher,
    private val dbHandleFactory: DbHandleFactory,
) {

    fun listApplications(): List<ApplicationED> {
        return dbHandleFactory.withHandle {
            configurationDao.listApplications(it)
        }
    }

    fun createApplication(requestId: String, appName: String): CreateApplicationResult {
        checkRequestId(requestId)
        return executeMutable(requestId, CreateApplicationResult.OK) {
            configurationDao.createApplication(it, appName)
        }
    }

    fun deleteApplication(requestId: String, applicationName: String): DeleteApplicationResult {
        checkRequestId(requestId)
        return executeMutable(requestId, DeleteApplicationResult.OK) {
            configurationDao.deleteApplication(it, applicationName)
        }
    }

    fun updateApplication(requestId: String, id: ApplicationId, applicationName: String): UpdateApplicationResult {
        checkRequestId(requestId)
        return executeMutable(requestId, UpdateApplicationResult.OK) {
            configurationDao.updateApplication(it, id, applicationName)
        }
    }

    fun createHost(requestId: String, hostName: String): HostCreateResult {
        checkRequestId(requestId)
        return executeMutable(requestId, HostCreateResult.OK) {
            configurationDao.createHost(it, hostName)
        }
    }

    fun listHosts(): List<HostED> {
        return dbHandleFactory.withHandle {
            configurationDao.listHosts(it)
        }
    }

    fun updateProperty(
        requestId: String,
        appName: String,
        hostName: String,
        propertyName: String,
        value: String,
        version: Long?,
    ): PropertyCreateResult {
        checkRequestId(requestId)
        return executeMutable(requestId, PropertyCreateResult.OK) {
            configurationDao.updateProperty(it, appName, propertyName, value, version, hostName)
        }
    }

    fun deleteProperty(
        requestId: String,
        appName: String,
        hostName: String,
        propertyName: String,
        version: Long,
    ): DeletePropertyResult {
        checkRequestId(requestId)
        return executeMutable(requestId, DeletePropertyResult.OK) {
            configurationDao.deleteProperty(it, appName, hostName, propertyName, version)
        }
    }

    fun subscribeToApplication(
        subscriberId: String,
        defaultApplicationName: String,
        hostName: String,
        applicationName: String,
        lastKnownVersion: Long,
        subscriber: WatchSubscriber,
    ): PropertiesChanges? {
        return propertiesWatchDispatcher.subscribeToApplication(
            subscriberId,
            defaultApplicationName,
            hostName,
            applicationName,
            lastKnownVersion,
            subscriber
        )
    }

    fun searchProperties(searchPropertyRequest: SearchPropertyRequest): List<PropertyItemED> {
        return dbHandleFactory.withHandle {
            configurationDao.searchProperties(it, searchPropertyRequest)
        }
    }

    fun listProperties(applicationName: String): List<String> {
        return dbHandleFactory.withHandle {
            configurationDao.listProperties(it, applicationName)
        }
    }

    fun updateLastVersion(subscriberId: String, applicationName: String, version: Long) {
        propertiesWatchDispatcher.updateVersion(subscriberId, applicationName, version)
    }

    fun unsubscribe(subscriberId: String) {
        propertiesWatchDispatcher.unsubscribe(subscriberId)
    }

    private fun checkRequestId(requestId: String) {
        if (requestId.isEmpty()) {
            throw IllegalArgumentException("RequestId is empty")
        }
    }

    fun readProperty(applicationName: String, hostName: String, propertyName: String): PropertyItemED? {
        return dbHandleFactory.withHandle {
            configurationDao.readProperty(it, hostName, propertyName, applicationName)
        }
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

sealed class DeleteApplicationResult {
    object OK : DeleteApplicationResult()
    object ApplicationNotFound : DeleteApplicationResult()
}

sealed class UpdateApplicationResult {
    object OK : UpdateApplicationResult()
    object ApplicationNotFound : UpdateApplicationResult()
}

sealed class CreateApplicationResult {
    object OK : CreateApplicationResult()
    object ApplicationAlreadyExists : CreateApplicationResult()
}

data class ApplicationED(
    val id: ApplicationId,
    val name: String,
    val lastVersion: Long,
    val createdMs: Long,
    val modifiedMs: Long,
)

data class HostED(
    val id: Long?,
    val name: String,
    val createdMs: Long,
    val modifiedMs: Long,
)

data class SearchPropertyRequest(
    val applicationName: String?,
    val propertyNameQuery: String?,
    val propertyValueQuery: String?,
    val hostNameQuery: String?,
)

data class TableMetaED(val version: Long)
