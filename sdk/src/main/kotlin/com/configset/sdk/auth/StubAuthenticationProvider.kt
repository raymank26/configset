package com.configset.sdk.auth

import java.time.Instant

class StubAuthenticationProvider private constructor(
    private val tokenToUser: Map<String, UserInfo>
) : AuthenticationProvider {

    override fun authenticate(accessToken: String): UserInfo {
        return tokenToUser[accessToken] ?: error("Not in mapping $accessToken")
    }

    companion object {

        fun stubAuthenticationProvider(config: StubAuthenticationProviderBuilder.() -> Unit):
                StubAuthenticationProvider {
            val builder = StubAuthenticationProviderBuilder(mutableMapOf())
            config(builder)
            return StubAuthenticationProvider(builder.tokenToUser)
        }

        class StubAuthenticationProviderBuilder(val tokenToUser: MutableMap<String, UserInfo>) {

            fun addUser(accessToken: String, username: String, roles: Set<String>) {
                tokenToUser[accessToken] = object : UserInfo {
                    override val accessToken: String = accessToken
                    override val userName: String = username
                    override val roles: Set<String> = roles
                    override fun accessTokenExpired(instant: Instant): Boolean = false
                }
            }
        }
    }
}
