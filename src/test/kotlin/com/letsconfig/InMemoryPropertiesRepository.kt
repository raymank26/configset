package com.letsconfig

class InMemoryPropertiesRepository(var propertiesChanges: PropertiesChanges) : PropertiesRepository {

    private val diffPropertiesChanges: MutableMap<DiffPair, PropertiesChanges> = mutableMapOf()

    init {
        diffPropertiesChanges[DiffPair(null, propertiesChanges.lastVersion)] = propertiesChanges
    }

    override fun getUpdatedProperties(fromVersion: Long?, toVersion: Long): PropertiesChanges {
        return diffPropertiesChanges[DiffPair(fromVersion, toVersion)]!!
    }

    override fun getUpdatedProperties(): PropertiesChanges {
        return propertiesChanges
    }

    fun putPropertiesChanges(fromVersion: Long?, toVersion: Long, propertiesChanges: PropertiesChanges) {
        diffPropertiesChanges[DiffPair(fromVersion, toVersion)] = propertiesChanges
    }
}

private data class DiffPair(val fromVersion: Long?, val toVersion: Long)