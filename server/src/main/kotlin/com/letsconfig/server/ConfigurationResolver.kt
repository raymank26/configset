package com.letsconfig.server

import com.letsconfig.sdk.extension.createLoggerStatic
import com.letsconfig.server.db.ConfigurationApplication
import com.letsconfig.server.db.ConfigurationProperty

private val LOG = createLoggerStatic<ConfigurationResolver>()

class ConfigurationResolver {

    fun getChanges(snapshot: Map<String, ConfigurationApplication>, app: String, hostName: String,
                   defaultApplication: String, lastVersion: Long?): PropertiesChanges? {

        val properties: ConfigurationApplication = snapshot[app] ?: return null

        val collectedProperties = mutableListOf<PropertyItem>()

        if (LOG.isDebugEnabled) {
            LOG.debug("Snapshot = $snapshot, app = $app, hostName = $hostName, defaultApplication = $defaultApplication," +
                    "lastVersion = $lastVersion")
        }

        var newLastVersion = lastVersion
        for (config: ConfigurationProperty in properties.config.values) {
            for (targetHostName: String in listOf(hostName, "host-$app", "host-$defaultApplication")) {
                val item: PropertyItem? = config.hosts[targetHostName]
                if (item != null && (lastVersion == null || lastVersion < item.version)) {
                    collectedProperties.add(item)
                    newLastVersion = item.version.coerceAtLeast(newLastVersion ?: -1)
                    break
                }
            }
        }
        val changes = if (newLastVersion == null) {
            null
        } else {
            PropertiesChanges(newLastVersion, collectedProperties)
        }
        if (LOG.isDebugEnabled) {
            LOG.debug("Result = $changes")
        }
        return changes
    }
}

data class PropertiesChanges(val lastVersion: Long, val propertyItems: List<PropertyItem>)