package com.configset.server.auth

import com.auth0.jwt.interfaces.JWTVerifier
import com.configset.sdk.extension.createLoggerStatic
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

private val log = createLoggerStatic<OAuth2Authenticator>()

class OAuth2Authenticator(
    private val verificationAlgorithm: JWTVerifier,
) : Authenticator {

    override fun getUserInfo(accessToken: String): UserInfo {
        return try {
            val decodedJwt = verificationAlgorithm.verify(accessToken)
            val rolesJson: RolesJson = decodedJwt.claims["realm_access"]!!.`as`(RolesJson::class.java)

            LoggedIn(decodedJwt.claims["preferred_username"]!!.asString(), rolesJson.roles.toHashSet())
        } catch (e: Exception) {
            log.info("Unable to process access_token", e)
            Anonymous
        }
    }
}

private data class RolesJson @JsonCreator constructor(@JsonProperty("roles") val roles: List<String>)