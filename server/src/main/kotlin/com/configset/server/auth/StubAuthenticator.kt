package com.configset.server.auth

class StubAuthenticator(private val tokenToUser: Map<String, UserInfo>) : Authenticator {
    override fun getUserInfo(accessToken: String): UserInfo {
        return tokenToUser[accessToken] ?: error("Not in mapping $accessToken")
    }
}