package com.configset.sdk

data class ApplicationId(val id: Long) {
    constructor(id: String) : this(id.toLong())
}