package com.configset.common.backend.auth

interface AuthenticationProvider {

    fun authenticate(accessToken: String): UserInfo?
}
