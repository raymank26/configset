package com.letsconfig

class InMemoryPropertiesRepository(propertiesChanges: PropertiesChanges) : PropertiesRepository {

    private val diffPropertiesChanges: MutableMap<Version, PropertiesChanges> = mutableMapOf()

    init {
        diffPropertiesChanges[Version(null)] = propertiesChanges
    }

    override fun getUpdatedProperties(fromVersion: Long?): PropertiesChanges {
        return diffPropertiesChanges[Version(fromVersion)]!!
    }

    fun putPropertiesChanges(fromVersion: Long?, propertiesChanges: PropertiesChanges) {
        diffPropertiesChanges[Version(fromVersion)] = propertiesChanges
    }
}

private data class Version(val version: Long?)