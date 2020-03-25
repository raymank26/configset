package com.letsconfig.model

interface PropertiesObserver {
    fun handleChanges(propertiesChanges: PropertiesChanges)
    fun lastKnownVersion(): Long?
}