package com.letsconfig.model

sealed class PropertyItem {
    data class Updated(val applicationName: String, val name: String, val value: String) : PropertyItem()
    data class Deleted(val applicationName: String, val name: String) : PropertyItem()
}
