package com.configset.sdk.auth

interface AuthenticationProvider {

    fun authenticate(accessToken: String): UserInfo?
}