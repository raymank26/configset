package com.letsconfig

import com.letsconfig.model.PropertiesChanges

class InMemoryPropertiesRepository(propertiesChanges: PropertiesChanges) : PropertiesRepository {

    private val diffPropertiesChanges: MutableMap<Version, PropertiesChanges> = mutableMapOf()
    private val singleObservable = SingleObservable<PropertiesChanges>(null)

    init {
        diffPropertiesChanges[Version(null)] = propertiesChanges
    }

    override fun getUpdatedProperties(fromVersion: Long?): PropertiesChanges {
        return diffPropertiesChanges[Version(fromVersion)]!!
    }

    override fun subscribe(onChanges: (PropertiesChanges) -> Unit) {
        singleObservable.subscribe(onChanges)
    }

    fun pushChanges(propertiesChanges: PropertiesChanges) {
        singleObservable.handle(propertiesChanges)
    }
}

private data class Version(val version: Long?)