package com.letsconfig

import com.letsconfig.db.ConfigurationApplication
import com.letsconfig.db.ConfigurationProperty

class ConfigurationResolver {

    fun getProperties(snapshot: Map<String, ConfigurationApplication>, app: String, hostName: String,
                      defaultHostName: String, lastVersion: Long?): ResolvedConfig {

        val properties: ConfigurationApplication = snapshot[app] ?: return ResolvedConfig(null, emptyList())

        val result = mutableListOf<PropertyItem>()

        var newLastVersion = lastVersion
        for (config: ConfigurationProperty in properties.config.values) {
            for (targetHostName: String in listOf(hostName, "host-$app", defaultHostName)) {
                val item: PropertyItem? = config.hosts[targetHostName]
                if (item != null && (lastVersion == null || lastVersion < item.version)) {
                    result.add(item)
                    newLastVersion = item.version.coerceAtLeast(newLastVersion ?: -1)
                    break
                }
            }
        }
        return ResolvedConfig(newLastVersion, result)
    }
}

data class ResolvedConfig(val lastVersion: Long?, val propertyItems: List<PropertyItem>)