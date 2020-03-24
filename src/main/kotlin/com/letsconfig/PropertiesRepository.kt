package com.letsconfig

interface PropertiesRepository {
    fun getUpdatedProperties(fromVersion: Long?): PropertiesChanges
    fun subscribe(onChanges: (PropertiesChanges) -> Unit)
}