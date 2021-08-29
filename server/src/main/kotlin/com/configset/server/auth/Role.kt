package com.configset.server.auth


sealed interface Role {
    val key: String
    val parent: Role?
        get() = null
}

object Admin : Role {
    override val key: String = "admin"
}

data class ApplicationOwner(val appName: String) : Role {
    override val key: String = "applicationOwner_$appName"
    override val parent: Role = Admin
}