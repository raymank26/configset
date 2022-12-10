package com.configset.common.client

data class ApplicationId(val id: Long) {
    constructor(id: String) : this(id.toLong())
}
