package com.configset.server

import com.configset.sdk.extension.createLoggerStatic
import com.configset.server.db.ConfigurationApplication
import com.configset.server.db.ConfigurationProperty
import com.configset.server.db.PropertyItemED

private val LOG = createLoggerStatic<ConfigurationResolver>()

class ConfigurationResolver {

    fun getChanges(
        snapshot: Map<String, ConfigurationApplication>,
        targetApplication: String,
        hostName: String,
        defaultApplication: String,
        lastVersion: Long,
    ): PropertiesChanges? {

        val properties: ConfigurationApplication = snapshot[targetApplication] ?: return null

        val collectedProperties = mutableListOf<PropertyItemED>()

        if (LOG.isDebugEnabled) {
            LOG.debug("Snapshot = $snapshot, app = $targetApplication, hostName = $hostName, defaultApplication = $defaultApplication," +
                    "lastVersion = $lastVersion")
        }

        var newLastVersion = lastVersion
        for (config: ConfigurationProperty in properties.config.values) {
            val propertyItem: PropertyItemED? =
                resolveProperty(config.hosts, hostName, defaultApplication, targetApplication)
            if (propertyItem != null && (lastVersion < propertyItem.version)) {
                collectedProperties.add(propertyItem)
                newLastVersion = propertyItem.version.coerceAtLeast(newLastVersion)
            }
        }
        val changes = PropertiesChanges(newLastVersion, collectedProperties)
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

data class PropertiesChanges(val lastVersion: Long, val propertyItems: List<PropertyItemED>)