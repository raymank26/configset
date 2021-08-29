package com.configset.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.configset.sdk.extension.createLoggerStatic
import com.configset.server.util.retry
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

private val log = createLoggerStatic<OAuth2Authenticator>()

class OAuth2Authenticator(
    private val oauth2Api: OAuth2Api,
    private val verificationBuilder: JwtVerificationBuilder,
) : Authenticator {

    private lateinit var verifier: JWTVerifier

    fun init() {
        setupVerifier()
    }

    private fun setupVerifier() {
        val realmInfo = retry({ oauth2Api.getResource() })
        val publicKeyBytes = Base64.getDecoder().decode(realmInfo.publicKey)
        val encodedKeySpec = X509EncodedKeySpec(publicKeyBytes)
        val kf = KeyFactory.getInstance("RSA")
        val publicKey = kf.generatePublic(encodedKeySpec) as RSAPublicKey
        val jwtAlgo = Algorithm.RSA256(publicKey, null)
        verifier = verificationBuilder.build(jwtAlgo)
    }


    override fun getUserInfo(accessToken: String): UserInfo {
        return try {
            val decodedJwt = verifier.verify(accessToken)
            val rolesJson: RolesJson = decodedJwt.claims["realm_access"]!!.`as`(RolesJson::class.java)

            LoggedIn(decodedJwt.claims["preferred_username"]!!.asString(), rolesJson.roles.toHashSet())
        } catch (e: Exception) {
            log.info("Unable to process access_token", e)
            Anonymous
        }
    }
}

fun interface JwtVerificationBuilder {
    fun build(algo: Algorithm): JWTVerifier
}

class DefaultJwtVerificationBuilder : JwtVerificationBuilder {
    override fun build(algo: Algorithm): JWTVerifier {
        return JWT.require(algo).build()
    }
}

private data class RolesJson @JsonCreator constructor(@JsonProperty("roles") val roles: List<String>)