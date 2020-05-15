package com.letsconfig.server

import com.letsconfig.sdk.extension.createLoggerStatic
import com.letsconfig.server.db.ConfigurationApplication
import com.letsconfig.server.db.ConfigurationProperty

private val LOG = createLoggerStatic<ConfigurationResolver>()

class ConfigurationResolver {

    fun getChanges(snapshot: Map<String, ConfigurationApplication>, targetApplication: String, hostName: String,
                   defaultApplication: String, lastVersion: Long?): PropertiesChanges? {

        val properties: ConfigurationApplication = snapshot[targetApplication] ?: return null

        val collectedProperties = mutableListOf<PropertyItem>()

        if (LOG.isDebugEnabled) {
            LOG.debug("Snapshot = $snapshot, app = $targetApplication, hostName = $hostName, defaultApplication = $defaultApplication," +
                    "lastVersion = $lastVersion")
        }

        var newLastVersion = lastVersion
        for (config: ConfigurationProperty in properties.config.values) {
            val propertyItem: PropertyItem? = resolveProperty(config.hosts, hostName, defaultApplication, targetApplication)
            if (propertyItem != null && (lastVersion == null || lastVersion < propertyItem.version)) {
                collectedProperties.add(propertyItem)
                newLastVersion = propertyItem.version.coerceAtLeast(newLastVersion ?: -1)
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

fun <T> resolveProperty(configMap: Map<String, T>, hostName: String, defaultApplication: String, targetApplication: String): T? {
    val exact = configMap[hostName]
    if (exact != null) {
        return exact
    }
    val parts = hostName.split(".")
    for (i in 1 until parts.size) {
        val partialResult = configMap[parts.subList(i, parts.size).joinToString(".")]
        if (partialResult != null) {
            return partialResult
        }
    }
    return configMap["host-$defaultApplication"] ?: configMap["host-$targetApplication"]
}

data class PropertiesChanges(val lastVersion: Long, val propertyItems: List<PropertyItem>)