package com.letsconfig

interface PropertiesObserver {
    fun handleChanges(propertiesChanges: PropertiesChanges)
    fun lastKnownVersion(): Long?
}