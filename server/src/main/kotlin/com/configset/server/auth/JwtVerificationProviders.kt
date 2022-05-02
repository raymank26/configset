package com.configset.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Verification
import com.configset.server.util.retry
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

private val objectMapper = ObjectMapper()

class RemoteJwtVerificationProvider(
    private val httpClient: HttpClient,
    private val realmUrl: String,
) {

    fun createVerification(): Verification {
        val response = retry(maxRetries = Integer.MAX_VALUE) {
            httpClient.send(HttpRequest.newBuilder().GET()
                .uri(URI(realmUrl))
                .build(), HttpResponse.BodyHandlers.ofString())
        }
        val tree = objectMapper.readTree(response.body())
        val publicKeyContent = tree["public_key"].asText()

        return JwtVerificationProvider(publicKeyContent).createVerification()
    }
}

class JwtVerificationProvider(
    private val publicKeyContent: String,
) {

    fun createVerification(): Verification {
        val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent))
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(keySpec) as RSAPublicKey
        return JWT.require(Algorithm.RSA256(publicKey, null))
    }
}
//class JwtVerificationProvider(
//    private val httpClient: HttpClient,
//    private val realmUrl: String,
//) {
//
//    fun createVerifier(): JWTVerifier {
//        val response = retry(maxRetries = Integer.MAX_VALUE) {
//            httpClient.send(HttpRequest.newBuilder().GET()
//                .uri(URI(realmUrl))
//                .build(), HttpResponse.BodyHandlers.ofString())
//        }
//        val tree = objectMapper.readTree(response.body())
//        val publicKeyContent = tree["public_key"].asText()
//
//        val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent))
//        val publicKey = KeyFactory.getInstance("RSA")
//            .generatePublic(keySpec) as RSAPublicKey
//        return JWT.require(Algorithm.RSA256(publicKey, null))
//            .build()
//    }
//}
