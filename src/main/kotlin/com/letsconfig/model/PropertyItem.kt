package com.letsconfig.model

sealed class PropertyItem {
    data class Updated(val applicationName: String, val name: String, val value: String, val version: Long) : PropertyItem()
    data class Deleted(val applicationName: String, val name: String, val version: Long) : PropertyItem()
}
