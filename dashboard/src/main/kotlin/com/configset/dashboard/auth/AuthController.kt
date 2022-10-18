package com.configset.dashboard.auth

import com.auth0.jwt.JWT
import com.configset.dashboard.AuthenticationConfig
import com.configset.dashboard.util.urlEncode
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.Cookie
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Base64

class AuthController(
    private val authenticationConfig: AuthenticationConfig,
    private val objectMapper: ObjectMapper,
) {

    private val httpClient = OkHttpClient()

    fun bind() {
        get("/auth/redirect") { ctx ->
            val code = ctx.queryParam("code")!!
            val response = httpClient.newCall(
                Request.Builder()
                    .post(
                        FormBody.Builder()
                            .add("code", code)
                            .add("client_id", authenticationConfig.authClientId)
                            .add("client_secret", authenticationConfig.authSecretKey)
                            .add("redirect_uri", authenticationConfig.authRedirectUri)
                            .add("grant_type", "authorization_code")
                            .build()
                    )
                    .url(
                        authenticationConfig.requestTokenUri.toHttpUrl()
                            .newBuilder()
                            .build()
                    ).build()
            ).execute()
            if (response.code != 200) {
                ctx.redirect("/")
                return@get
            }
            val responseJson = response.body!!.byteStream().use {
                objectMapper.readTree(it)
            }
            val accessToken = responseJson.get("access_token").asText()

            val idToken = responseJson.get("id_token").asText()
            val idTokenDecoded = JWT.decode(idToken)
            val payloadJson = String(Base64.getDecoder().decode(idTokenDecoded.payload))
            val username = readFirst(
                objectMapper.readTree(payloadJson),
                setOf("preferred_username", "name", "nickname")
            )

            ctx.cookie(Cookie("auth.access_token", accessToken).apply {
                isHttpOnly = true
            })

            ctx.cookie(Cookie("auth.username", username.urlEncode()))

            ctx.redirect("/")
        }
    }

    private fun readFirst(jsonNode: JsonNode, names: Set<String>): String {
        for (name in names) {
            val value = jsonNode.get(name)
            if (value != null) {
                return value.asText()
            }
        }
        error("Cannot find property by names = $names")
    }
}