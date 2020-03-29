package com.letsconfig

import com.letsconfig.db.ConfigurationDao

class PropertiesWatchDispatcher(
        private val configurationDao: ConfigurationDao,
        private val configurationResolver: ConfigurationResolver,
        private val scheduler: Scheduler
) {

    private val subscriptions: MutableMap<SubscriberId, ObserverState> = mutableMapOf()

    private var configurationSnapshot: Map<String, List<PropertyItem>> = mapOf()

    fun start() {
        scheduler.scheduleWithFixedDelay(0, 5000) {
            update()
        }
    }

    @Synchronized
    fun subscribeApplication(subscriberId: String, defaultApplication: String, hostName: String, applicationName: String,
                             lastKnownVersion: Long?): List<PropertyItem> {

        val defaultHostName = toDefaultHostname(defaultApplication)
        val config = configurationResolver.getProperties(configurationSnapshot, applicationName, hostName,
                defaultApplication, lastKnownVersion)

        subscriptions.compute(subscriberId) { _, value ->
            val newAppState = ApplicationState(applicationName, lastKnownVersion)
            if (value == null) {
                ObserverState(hostName = hostName, defaultHostName = defaultHostName, applications = setOf(newAppState),
                        watchSubscriber = null)
            } else {
                require(hostName == value.hostName)
                require(defaultApplication == value.defaultHostName)
                val updatedApps = value.applications.filter { it.appName != applicationName }.plus(newAppState).toSet()
                ObserverState(hostName = hostName, defaultHostName = defaultApplication, applications = updatedApps,
                        watchSubscriber = null)
            }
        }
        return config.propertyItems
    }

    @Synchronized
    fun watchChanges(subscriber: WatchSubscriber) {
        subscriptions.compute(subscriber.getId()) { _, value ->
            require(value != null)
            value.copy(watchSubscriber = subscriber)
        }
    }

    @Synchronized
    fun unsubscribe(subscriberId: String) {
        subscriptions.remove(subscriberId)
    }

    @Synchronized
    private fun update() {
        configurationSnapshot = configurationDao.getConfigurationSnapshot()
        pushToClients()
    }

    private fun pushToClients() {
        for (observerState: ObserverState in subscriptions.values) {
            if (observerState.watchSubscriber != null) {
                continue
            }
            for (appState: ApplicationState in observerState.applications) {
                val changes = configurationResolver.getProperties(configurationSnapshot, appState.appName,
                        observerState.defaultHostName, observerState.hostName, appState.lastKnownVersion)
                for (propertyItem in changes.propertyItems) {
                    observerState.watchSubscriber!!.pushChanges(propertyItem)
                }
            }
        }
    }

    private fun toDefaultHostname(defaultApplicationName: String): String {
        return "host-$defaultApplicationName"
    }
}

private typealias SubscriberId = String

private data class ObserverState(
        val hostName: String,
        val defaultHostName: String,
        val applications: Set<ApplicationState>,
        val watchSubscriber: WatchSubscriber?
)

private data class ApplicationState(val appName: String, val lastKnownVersion: Long?)

