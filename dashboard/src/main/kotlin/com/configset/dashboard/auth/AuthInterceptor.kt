package com.configset.dashboard.auth

import com.auth0.jwt.JWT
import com.configset.dashboard.AuthenticationConfig
import io.javalin.http.Context
import io.javalin.http.Handler
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

class AuthInterceptor(
    private val excludePaths: List<String>,
    private val authenticationConfig: AuthenticationConfig,
) : Handler {

    override fun handle(ctx: Context) {
        for (excludePath in excludePaths) {
            if (ctx.path() == excludePath) {
                return
            }
        }

        val validAccessToken = getValidAccessToken(ctx)
        if (validAccessToken == null) {
            val redirectUriEncoded = URLEncoder.encode(authenticationConfig.authRedirectUri, StandardCharsets.UTF_8)
            val scopeEncoded = URLEncoder.encode("openid name", StandardCharsets.UTF_8)
            return ctx.redirect(buildString {
                append(authenticationConfig.authUri)
                append("?client_id=")
                append(authenticationConfig.authClientId)
                append("&scope=")
                append(scopeEncoded)
                append("&redirect_uri=")
                append(redirectUriEncoded)
            })
        } else {
            ctx.attribute("access_token", validAccessToken)
        }
    }

    private fun getValidAccessToken(ctx: Context): String? {
        return ctx.cookie("auth.access_token")?.let {
            val jwt = JWT.decode(it)
            if (jwt.expiresAt?.let { exp -> exp < Date() } != false) null else it
        }
    }
}