package com.letsconfig

class ConfigurationResolver {

    fun getProperties(snapshot: Map<String, List<PropertyItem>>, app: String, hostName: String, defaultHostName: String,
                      lastVersion: Long?): ResolvedConfig {

        val properties = snapshot[app] ?: return ResolvedConfig(emptyList())

        val byPropertyName: Collection<List<PropertyItem>> = properties.groupBy { it.name }.values

        val result = mutableListOf<PropertyItem>()
        for (props: List<PropertyItem> in byPropertyName) {
            val byHostsProperties: Map<String, PropertyItem> = props.associateBy { it.hostName }

            for (targetHostName in listOf(hostName, defaultHostName, "host-$app")) {
                val item: PropertyItem? = byHostsProperties[targetHostName]
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