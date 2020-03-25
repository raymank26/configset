package com.letsconfig

import com.letsconfig.model.PropertiesChanges

interface PropertiesRepository {
    fun getUpdatedProperties(fromVersion: Long?): PropertiesChanges
    fun subscribe(onChanges: (PropertiesChanges) -> Unit)
}