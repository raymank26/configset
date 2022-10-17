package com.configset.common.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.Verification
import com.configset.sdk.retry
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class RemoteAuthenticationProvider(
    private val httpClient: OkHttpClient,
    private val realmUrl: String,
    private val objectMapper: ObjectMapper,
    private val clientId: String,
) : AuthenticationProvider {

    private val publicKeyContent = lazy<String> {
        retry(maxRetries = Integer.MAX_VALUE) {
            httpClient.newCall(
                Request.Builder()
                    .get()
                    .url(realmUrl)
                    .build()
            ).execute().use { response ->
                val tree = objectMapper.readTree(response.body!!.byteStream())
                require(tree.get("public_key") != null) { "Cannot find `public_key` in OAuth provider response" }
                tree["public_key"].asText()
            }
        }
    }

    override fun authenticate(accessToken: String): UserInfo? {
        return try {

            val decodedToken = createVerification().build().verify(accessToken)
            val payloadJson = String(Base64.getDecoder().decode(decodedToken.payload))
            val username = readFirst(
                objectMapper.readTree(payloadJson),
                setOf("preferred_username", "name", "nickname")
            )
            JwtAuthentication(decodedToken, clientId, username, accessToken)
        } catch (e: TokenExpiredException) {
            null
        } catch (e: SignatureVerificationException) {
            null
        }
    }

    private fun createVerification(): Verification {
        val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent.value))
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(keySpec) as RSAPublicKey
        return JWT.require(Algorithm.RSA256(publicKey, null))
    }

    private fun readFirst(jsonNode: JsonNode, names: Set<String>): String {
        for (name in names) {
            val value = jsonNode.get(name)
            if (value != null) {
                return value.asText()
            }
        }
        error("Cannot find property given names = $names")
    }
}
