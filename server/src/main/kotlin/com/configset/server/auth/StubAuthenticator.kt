package com.configset.server.auth

import com.configset.sdk.auth.AuthenticationProvider
import com.configset.sdk.auth.UserInfo

class StubAuthenticator(private val tokenToUser: Map<String, UserInfo>) : AuthenticationProvider {

    override fun authenticate(accessToken: String): UserInfo {
        return tokenToUser[accessToken] ?: error("Not in mapping $accessToken")
    }
}