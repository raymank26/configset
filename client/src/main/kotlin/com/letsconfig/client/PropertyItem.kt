package com.letsconfig.client

sealed class PropertyItem {

    abstract val applicationName: String
    abstract val name: String
    abstract val version: Long

    data class Updated(
            override val applicationName: String,
            override val name: String,
            override val version: Long,
            val value: String
    ) : PropertyItem()

    data class Deleted(
            override val applicationName: String,
            override val name: String,
            override val version: Long
    ) : PropertyItem()
}
