package com.letsconfig

class InMemoryPropertiesObserver(private val lastKnownVersion: Long?) : PropertiesObserver {

    var propertiesChanges: PropertiesChanges? = null
        private set


    override fun handleChanges(propertiesChanges: PropertiesChanges) {
        this.propertiesChanges = propertiesChanges
    }

    override fun lastKnownVersion(): Long? {
        return lastKnownVersion
    }
}