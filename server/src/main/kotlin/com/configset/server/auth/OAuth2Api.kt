package com.configset.server.auth

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

interface OAuth2Api {
    fun getResource(): ResourceInfo
}

class OAuth2Rest2Api(private val baseUrl: String, private val timeoutMs: Long) : OAuth2Api {
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build()
    private val om = ObjectMapper()

    override fun getResource(): ResourceInfo {
        val body: HttpResponse<String> = httpClient.send(HttpRequest.newBuilder()
            .GET()
            .timeout(Duration.ofMillis(timeoutMs))
            .uri(URI(baseUrl))
            .build(), HttpResponse.BodyHandlers.ofString())
        require(body.statusCode() == 200)
        return ResourceInfo(om.readTree(body.body())["public_key"].asText())
    }
}

data class ResourceInfo(val publicKey: String)