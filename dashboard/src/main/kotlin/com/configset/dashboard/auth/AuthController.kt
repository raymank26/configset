package com.configset.dashboard.auth

import com.auth0.jwt.JWT
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.Cookie
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

class AuthController(
    private val authSecretKey: String,
    private val authClientId: String,
    private val requestTokenUri: String,
    private val authRedirectUri: String,
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
                            .add("client_id", authClientId)
                            .add("client_secret", authSecretKey)
                            .add("redirect_uri", authRedirectUri)
                            .add("grant_type", "authorization_code")
                            .build()
                    )
                    .url(
                        requestTokenUri.toHttpUrl()
                            .newBuilder()
                            .build()
                    ).build()
            ).execute()
            require(response.code == 200)
            val responseJson = response.body!!.byteStream().use {
                objectMapper.readTree(it)
            }
            val accessToken = responseJson.get("access_token").asText()
            ctx.cookie(Cookie("auth.access_token", accessToken).apply {
                isHttpOnly = true
            })

            val idToken = responseJson.get("id_token").asText()
            val idTokenDecoded = JWT.decode(idToken)
            val payloadJson = String(Base64.getDecoder().decode(idTokenDecoded.payload))
            val username = objectMapper.readTree(payloadJson).get("name")?.asText()
                ?: error("Cannot find 'name' property in token payload $payloadJson")
            ctx.cookie(Cookie("auth.username", username))

            ctx.redirect("/")
        }
    }
}