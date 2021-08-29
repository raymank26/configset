package com.configset.server.auth

interface Authenticator {
    fun getUserInfo(accessToken: String): UserInfo
}