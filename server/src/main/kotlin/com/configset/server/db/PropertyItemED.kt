package com.configset.server.db

data class PropertyItemED(
    val id: Long?,
    val name: String,
    val value: String,
    val hostName: String,
    val applicationName: String,
    val version: Long,
    val deleted: Boolean,
    val createdMs: Long,
    val modifiedMs: Long,
)
