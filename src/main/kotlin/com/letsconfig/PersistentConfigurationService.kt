package com.letsconfig

import com.letsconfig.db.ConfigurationDao

class PersistentConfigurationService(
        private val configurationDao: ConfigurationDao,
        private val propertiesWatchDispatcher: PropertiesWatchDispatcher
) : ConfigurationService {

    override fun listApplications(): List<String> {
        return configurationDao.listApplications()
    }

    override fun createApplication(appName: String): CreateApplicationResult {
        return configurationDao.createApplication(appName)
    }

    override fun createHost(hostName: String): HostCreateResult {
        return configurationDao.createHost(hostName)
    }

    override fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: String): PropertyCreateResult {
        return configurationDao.updateProperty(appName, hostName, propertyName, value, version)
    }

    override fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult {
        return configurationDao.deleteProperty(appName, hostName, propertyName)
    }

    override fun subscribeApplication(subscriberId: String, defaultApplicationName: String, hostName: String,
                                      applicationName: String, lastKnownVersion: Long?): List<PropertyItem> {
        return propertiesWatchDispatcher.subscribeApplication(subscriberId, defaultApplicationName, hostName, applicationName, lastKnownVersion)
    }

    override fun watchChanges(subscriber: WatchSubscriber) {
        propertiesWatchDispatcher.watchChanges(subscriber)
    }

    override fun unsubscribe(subscriberId: String) {
        propertiesWatchDispatcher.unsubscribe(subscriberId)
    }
}