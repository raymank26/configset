package com.letsconfig

interface PropertiesRepository {
    fun getUpdatedProperties(fromVersion: Long?, toVersion: Long): PropertiesChanges
    fun getUpdatedProperties(): PropertiesChanges
}