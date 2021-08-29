package com.configset.server.auth

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OAuth2RestApiTest {

    @Rule
    @JvmField
    val wireMockRule = WireMockRule(4233)

    private lateinit var oauth2RestApi: OAuth2Rest2Api

    @Before
    fun before() {
        oauth2RestApi = OAuth2Rest2Api(wireMockRule.baseUrl() + "/auth", timeoutMs = 10000)
    }

    @Test
    fun testRealmInfo() {
        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/auth")).willReturn(WireMock.aResponse()
            .withBody("{\"public_key\": \"some public key\"}")))
        oauth2RestApi.getResource()
    }
}