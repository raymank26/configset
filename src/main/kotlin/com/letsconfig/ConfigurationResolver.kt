package com.letsconfig

import com.letsconfig.db.ConfigurationApplication
import com.letsconfig.db.ConfigurationProperty

class ConfigurationResolver {

    fun getProperties(snapshot: Map<String, ConfigurationApplication>, app: String, hostName: String,
                      defaultHostName: String, lastVersion: Long?): ResolvedConfig {

        val properties: ConfigurationApplication = snapshot[app] ?: return ResolvedConfig(emptyList())

        val result = mutableListOf<PropertyItem>()

        for (config: ConfigurationProperty in properties.config.values) {
            for (targetHostName: String in listOf(hostName, "host-$app", defaultHostName)) {
                val item: PropertyItem? = config.hosts[targetHostName]
                if (item != null && (lastVersion == null || lastVersion < item.version)) {
                    result.add(item)
                    break
                }
            }
        }
        return ResolvedConfig(result)
    }
}

data class ResolvedConfig(val propertyItems: List<PropertyItem>)