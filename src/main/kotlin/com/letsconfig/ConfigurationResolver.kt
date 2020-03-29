package com.letsconfig

import com.letsconfig.db.ConfigurationApplication
import com.letsconfig.db.ConfigurationProperty

class ConfigurationResolver {

    fun getProperties(snapshot: Map<String, ConfigurationApplication>, app: String, hostName: String,
                      defaultHostName: String, lastVersion: Long?): ResolvedConfig {

        val properties: ConfigurationApplication = snapshot[app] ?: return ResolvedConfig(emptyList())

        val result = mutableListOf<PropertyItem>()

        for (config: ConfigurationProperty in properties.config.values) {
            for (targetHostName in listOf(hostName, defaultHostName, "host-$app")) {
                val item: PropertyItem? = config.hosts[targetHostName]
                if (item != null && (lastVersion == null || lastVersion < item.version)) {
                    result.add(item)
                    break
                }
            }
        }
        return ResolvedConfig(emptyList())
    }
}

data class ResolvedConfig(val propertyItems: List<PropertyItem>)