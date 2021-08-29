package com.configset.server.auth

interface UserInfo {
    val userName: String
    val roles: Set<String>
    val anonymous: Boolean
}

data class LoggedIn(override val userName: String, override val roles: Set<String>) : UserInfo {
    override val anonymous: Boolean = false
}

object Anonymous : UserInfo {
    override val userName: String = "Anonymous"
    override val roles: Set<String> = emptySet()
    override val anonymous: Boolean = true
}