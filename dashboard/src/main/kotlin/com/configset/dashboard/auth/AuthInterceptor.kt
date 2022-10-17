package com.configset.dashboard.auth

import com.configset.common.backend.auth.AuthenticationProvider
import com.configset.common.backend.auth.UserInfo
import com.configset.dashboard.AuthenticationConfig
import com.configset.dashboard.util.userInfoOrNull
import io.javalin.http.Context
import io.javalin.http.Handler
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

class AuthInterceptor(
    private val excludePaths: List<String>,
    private val authenticationConfig: AuthenticationConfig,
    private val authenticationProvider: AuthenticationProvider,
) : Handler {

    override fun handle(ctx: Context) {
        for (excludePath in excludePaths) {
            if (ctx.path() == excludePath) {
                return
            }
        }

        val userInfo = getValidUserInfoOrNull(ctx)
        if (userInfo == null) {
            val redirectUriEncoded = URLEncoder.encode(authenticationConfig.authRedirectUri, StandardCharsets.UTF_8)
            val scopeEncoded = URLEncoder.encode("openid profile", StandardCharsets.UTF_8)
            return ctx.redirect(buildString {
                append(authenticationConfig.authUri)
                append("?client_id=")
                append(authenticationConfig.authClientId)
                append("&scope=")
                append(scopeEncoded)
                append("&redirect_uri=")
                append(redirectUriEncoded)
                append("&response_type=code")
            })
        } else {
            ctx.attribute("user_info", userInfo)
        }
    }

    private fun getValidUserInfoOrNull(ctx: Context): UserInfo? {
        val accessToken = ctx.cookie("auth.access_token")
        val userInfo = ctx.userInfoOrNull();
        val now = Instant.now()
        if (accessToken != null
            && userInfo != null
            && accessToken == userInfo.accessToken
            && !userInfo.accessTokenExpired(now)
        ) {
            return userInfo
        }
        return accessToken?.let {
            return authenticationProvider.authenticate(accessToken)
        }
    }
}