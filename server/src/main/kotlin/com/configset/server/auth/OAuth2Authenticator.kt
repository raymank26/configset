package com.configset.server.auth

import com.auth0.jwt.JWT
import com.configset.sdk.extension.createLoggerStatic
import com.configset.server.util.retry
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val log = createLoggerStatic<OAuth2Authenticator>()

class OAuth2Authenticator(
    private val baseUrl: String,
    private val timeoutMs: Long,
    private val httpClient: HttpClient,
) : Authenticator {

    override fun getUserInfo(accessToken: String): UserInfo {
        retry {
            val body: HttpResponse<String> = httpClient.send(HttpRequest.newBuilder()
                .GET()
                .timeout(Duration.ofMillis(timeoutMs))
                .uri(URI(baseUrl))
                .build(), HttpResponse.BodyHandlers.ofString())
            require(body.statusCode() == 200)
        }

        return try {
            val decodedJwt = JWT.decode(accessToken)
            val rolesJson: RolesJson = decodedJwt.claims["realm_access"]!!.`as`(RolesJson::class.java)

            LoggedIn(decodedJwt.claims["preferred_username"]!!.asString(), rolesJson.roles.toHashSet())
        } catch (e: Exception) {
            log.info("Unable to process access_token", e)
            Anonymous
        }
    }
}

private data class RolesJson @JsonCreator constructor(@JsonProperty("roles") val roles: List<String>)