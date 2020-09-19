package com.configset.server

import com.configset.sdk.extension.createLoggerStatic
import com.configset.server.db.ConfigurationApplication
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.ConfigurationProperty

private val LOG = createLoggerStatic<PropertiesWatchDispatcher>()

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

        val changes = configurationResolver.getChanges(configurationSnapshot, applicationName, hostName,
                defaultApplication, lastKnownVersion)

        subscriptions.compute(subscriberId) { _, value ->
            val newAppState = ApplicationState(applicationName, lastKnownVersion)
            if (value == null) {
                ObserverState(hostName = hostName, defaultApplicationName = defaultApplication, applications = setOf(newAppState),
                        watchSubscriber = null)
            } else {
                require(hostName == value.hostName)
                require(defaultApplication == value.defaultApplicationName)
                val updatedApps = value.applications.filter { it.appName != applicationName }.plus(newAppState).toSet()
                ObserverState(hostName = hostName, defaultApplicationName = defaultApplication, applications = updatedApps,
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
        LOG.info("Subscriber with id = ${subscriber.getId()} is connected to watch")
    }

    @Synchronized
    fun unsubscribe(subscriberId: String) {
        subscriptions.remove(subscriberId)
    }

    @Synchronized
    private fun update() {
        val properties = configurationDao.getConfigurationSnapshotList()
        LOG.trace("Properties size in memory = ${properties.size}")
        configurationSnapshot = listToMapping(properties)
        pushToClients()
    }

    private fun listToMapping(properties: List<PropertyItem>): Map<String, ConfigurationApplication> {
        LOG.trace("Properties = $properties")
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
                        observerState.hostName, observerState.defaultApplicationName, appState.lastVersion)
                if (changes != null && changes.propertyItems.isNotEmpty()) {
                    if (LOG.isDebugEnabled) {
                        LOG.debug("Found changes with size = ${changes.propertyItems.size}" +
                                " and lastVersion = ${changes.lastVersion}" +
                                ", subscriberId = ${watchSubscriber.getId()}" +
                                ", prevVersion = ${appState.lastVersion}")
                    }
                    watchSubscriber.pushChanges(appState.appName, changes)
                }
            }
        }
    }

    @Synchronized
    fun updateVersion(subscriberId: String, applicationName: String, version: Long) {
        val subscription = subscriptions[subscriberId]
        if (subscription == null) {
            LOG.warn("Unable to find subscription for subscriber = $subscriberId")
            return
        }
        val appSubscription = subscription.applications.find { it.appName == applicationName }
        if (appSubscription == null) {
            LOG.warn("Unable to find app subscription for subscriber = $subscriberId, app = $applicationName")
            return
        }
        if (appSubscription.lastVersion != null && version <= appSubscription.lastVersion!!) {
            LOG.debug("Incoming version is obsolete for subscriber = $subscriberId, app = $applicationName")
            return
        }
        appSubscription.lastVersion = version
        LOG.debug("Version updated for for subscriber = $subscriberId, app = $applicationName")
    }
}

private typealias SubscriberId = String

private data class ObserverState(
        val hostName: String,
        val defaultApplicationName: String,
        val applications: Set<ApplicationState>,
        var watchSubscriber: WatchSubscriber?
)

private data class ApplicationState(val appName: String, var lastVersion: Long?)

