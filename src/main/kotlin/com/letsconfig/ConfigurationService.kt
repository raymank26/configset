package com.letsconfig

import com.letsconfig.db.ConfigurationDao

class ConfigurationService(
        private val configurationDao: ConfigurationDao,
        private val propertiesWatchDispatcher: PropertiesWatchDispatcher
) {

    fun listApplications(): List<ApplicationED> {
        return configurationDao.listApplications()
    }

    fun createApplication(appName: String): CreateApplicationResult {
        return configurationDao.createApplication(appName)
    }

    fun createHost(hostName: String): HostCreateResult {
        return configurationDao.createHost(hostName)
    }

    fun listHosts(): List<HostED> {
        return configurationDao.listHosts()
    }

    fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult {
        return configurationDao.updateProperty(appName, hostName, propertyName, value, version)
    }

    fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult {
        return configurationDao.deleteProperty(appName, hostName, propertyName)
    }

    fun subscribeApplication(subscriberId: String, defaultApplicationName: String, hostName: String,
                             applicationName: String, lastKnownVersion: Long?): List<PropertyItem> {
        return propertiesWatchDispatcher.subscribeApplication(subscriberId, defaultApplicationName, hostName, applicationName, lastKnownVersion)
    }

    fun watchChanges(subscriber: WatchSubscriber) {
        propertiesWatchDispatcher.watchChanges(subscriber)
    }

    fun unsubscribe(subscriberId: String) {
        propertiesWatchDispatcher.unsubscribe(subscriberId)
    }
}
interface WatchSubscriber {
    fun getId(): String
    fun pushChanges(change: PropertyItem)
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
    object PropertyNotFound : DeletePropertyResult()
}

data class ApplicationED(val id: Long?, val name: String, val lastVersion: Long, val createdMs: Long, val modifiedMs: Long)

data class HostED(val id: Long?, val name: String, val createdMs: Long, val modifiedMs: Long)
