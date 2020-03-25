package com.letsconfig

class InMemoryPropertiesObserver(private val lastKnownVersion: Long?) : PropertiesObserver {

    private val _propertiesChanges = mutableListOf<PropertyItem>()

    val propertiesChanges: List<PropertyItem> = _propertiesChanges

    override fun handleChanges(propertiesChanges: PropertiesChanges) {
        this._propertiesChanges.addAll(propertiesChanges.items)
    }

    override fun lastKnownVersion(): Long? {
        return lastKnownVersion
    }
}