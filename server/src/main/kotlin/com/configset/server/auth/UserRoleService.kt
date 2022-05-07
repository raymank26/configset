package com.configset.server.auth

class UserRoleService {

    fun hasRole(userInfo: UserInfo, role: Role): Boolean {
        var tmpRole: Role? = role
        while (tmpRole != null) {
            if (userInfo.roles.contains(tmpRole.key)) {
                return true
            }
            tmpRole = tmpRole.parent
        }
        return false
    }
}