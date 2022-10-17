package com.configset.common.backend.auth

import java.time.Instant

interface UserInfo {
    val accessToken: String
    val userName: String
    val roles: Set<Role>

    fun hasRole(role: Role): Boolean {
        var tmpRole: Role? = role
        while (tmpRole != null) {
            if (roles.contains(tmpRole)) {
                return true
            }
            tmpRole = tmpRole.parent
        }
        return false
    }

    fun hasRole(roleName: String): Boolean {
        return hasRole(parseRole(roleName))
    }

    fun accessTokenExpired(instant: Instant): Boolean
}