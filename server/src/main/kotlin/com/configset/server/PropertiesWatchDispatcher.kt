package com.configset.server

import com.configset.sdk.extension.createLoggerStatic
import com.configset.server.db.ConfigurationApplication
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.ConfigurationProperty
import com.configset.server.db.DbHandleFactory

private val LOG = createLoggerStatic<PropertiesWatchDispatcher>()

class PropertiesWatchDispatcher(
    private val configurationDao: ConfigurationDao,
    private val configurationResolver: ConfigurationResolver,
    private val dbHandleFactory: DbHandleFactory,
    private val scheduler: Scheduler,
    private val updateDelayMs: Long,
) {

    private val subscriptions: MutableMap<SubscriberId, ObserverState> = mutableMapOf()
    private var configurationSnapshot: MutableMap<String, ConfigurationApplication> = mutableMapOf()

    fun start() {
        scheduler.scheduleWithFixedDelay(updateDelayMs, updateDelayMs) {
            update()
        }
        update()
    }

    @Synchronized
    fun subscribeToApplication(
        subscriberId: String, defaultApplication: String, hostName: String, applicationName: String,
        lastKnownVersion: Long,
        subscriber: WatchSubscriber,
    ): PropertiesChanges? {

        val changes = configurationResolver.getChanges(
            configurationSnapshot, applicationName, hostName,
            defaultApplication, lastKnownVersion
        )

        subscriptions.compute(subscriberId) { _, value ->
            val newAppState = ApplicationState(applicationName, lastKnownVersion)
            if (value == null) {
                ObserverState(
                    hostName = hostName,
                    defaultApplicationName = defaultApplication,
                    applications = setOf(newAppState),
                    watchSubscriber = subscriber
                )
            } else {
                require(hostName == value.hostName)
                require(defaultApplication == value.defaultApplicationName)
                val updatedApps = value.applications.filter { it.appName != applicationName }.plus(newAppState).toSet()
                ObserverState(
                    hostName = hostName, defaultApplicationName = defaultApplication, applications = updatedApps,
                    watchSubscriber = subscriber
                )
            }
        }
        return changes
    }

    @Synchronized
    fun unsubscribe(subscriberId: String) {
        subscriptions.remove(subscriberId)
    }

    @Synchronized
    private fun update() {
        updateSnapshot()
        pushToClients()
    }

    private fun updateSnapshot() {
        val properties = dbHandleFactory.withHandle {
            configurationDao.getConfigurationSnapshotList(it)
        }
        LOG.debug("Properties size in memory = ${properties.size}")
        LOG.trace("Properties = $properties")
        configurationSnapshot = properties
            .groupBy { it.applicationName }
            .mapValues { entry ->
                val nameToByHost: Map<String, ConfigurationProperty> = entry.value
                    .groupBy { it.name }
                    .mapValues { prop ->
                        ConfigurationProperty(prop.key, prop.value.associateBy { it.hostName })
                    }
                ConfigurationApplication(entry.key, nameToByHost)
            }
            .toMutableMap()
    }

    private fun pushToClients() {
        for (observerState: ObserverState in subscriptions.values) {
            val watchSubscriber = observerState.watchSubscriber

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
        if (version <= appSubscription.lastVersion && version != 0L) {
            LOG.debug("Incoming version is obsolete for subscriber = $subscriberId, app = $applicationName")
            return
        }
        appSubscription.lastVersion = version
        LOG.debug("Version updated for subscriber = $subscriberId, app = $applicationName, version = $version")
    }

    @Synchronized
    fun clear() {
        subscriptions.clear()
        configurationSnapshot.clear()
    }
}

private typealias SubscriberId = String

private data class ObserverState(
    val hostName: String,
    val defaultApplicationName: String,
    val applications: Set<ApplicationState>,
    var watchSubscriber: WatchSubscriber,
)

private data class ApplicationState(val appName: String, var lastVersion: Long)

