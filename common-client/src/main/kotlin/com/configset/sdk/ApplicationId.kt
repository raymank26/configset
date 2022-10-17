package com.configset.sdk

@JvmInline
value class ApplicationId(val id: Long) {
    constructor(id: String) : this(id.toLong())
}