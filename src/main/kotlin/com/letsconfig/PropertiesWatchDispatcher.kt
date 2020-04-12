package com.letsconfig

import com.letsconfig.db.ConfigurationApplication
import com.letsconfig.db.ConfigurationDao
import com.letsconfig.db.ConfigurationProperty

class PropertiesWatchDispatcher(
        private val configurationDao: ConfigurationDao,
        private val configurationResolver: ConfigurationResolver,
        private val scheduler: Scheduler,
        private val updateDelayMs: Long
) {

    private val subscriptions: MutableMap<SubscriberId, ObserverState> = mutableMapOf()

    private var configurationSnapshot: Map<String, ConfigurationApplication> = mapOf()

    fun start() {
        scheduler.scheduleWithFixedDelay(updateDelayMs, updateDelayMs) {
            update()
        }
        update()
    }

    @Synchronized
    fun subscribeApplication(subscriberId: String, defaultApplication: String, hostName: String, applicationName: String,
                             lastKnownVersion: Long?): PropertiesChanges? {

        val defaultHostName = toDefaultHostname(defaultApplication)
        val changes = configurationResolver.getChanges(configurationSnapshot, applicationName, hostName,
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
        return changes
    }


    @Synchronized
    fun watchChanges(subscriber: WatchSubscriber) {
        subscriptions.compute(subscriber.getId()) { _, value ->
            require(value != null)
            value.watchSubscriber = subscriber
            value
        }
    }

    @Synchronized
    fun unsubscribe(subscriberId: String) {
        subscriptions.remove(subscriberId)
    }

    @Synchronized
    private fun update() {
        configurationSnapshot = listToMapping(configurationDao.getConfigurationSnapshotList())
        pushToClients()
    }

    private fun listToMapping(properties: List<PropertyItem>): Map<String, ConfigurationApplication> {
        return properties
                .groupBy { it.applicationName }
                .mapValues { entry ->
                    val nameToByHost: Map<String, ConfigurationProperty> = entry.value
                            .groupBy { it.name }
                            .mapValues { prop ->
                                ConfigurationProperty(prop.key, prop.value.associateBy { it.hostName })
                            }
                    ConfigurationApplication(entry.key, nameToByHost)
                }
    }

    private fun pushToClients() {
        for (observerState: ObserverState in subscriptions.values) {
            val watchSubscriber = observerState.watchSubscriber ?: continue

            for (appState: ApplicationState in observerState.applications) {
                val changes = configurationResolver.getChanges(configurationSnapshot, appState.appName,
                        observerState.hostName, observerState.defaultHostName, appState.lastVersion)
                if (changes != null && changes.propertyItems.isNotEmpty()) {
                    watchSubscriber.pushChanges(changes)
                    appState.lastVersion = changes.lastVersion
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
        var watchSubscriber: WatchSubscriber?
)

private data class ApplicationState(val appName: String, var lastVersion: Long?)

