package com.configset.dashboard

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.WebDriverRunner
import com.configset.dashboard.pages.LeftNavPage
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.jsonResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.temporaryRedirect
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.amshove.kluent.shouldMatchAtLeastOneOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.util.*

class AuthorisationTest : FunctionalTest() {

    @JvmField
    @Rule
    var wireMockRule: WireMockRule = WireMockRule(23982)

    @Before
    fun setupWiremock() {
        wireMockRule.stubFor(
            get(urlPathEqualTo("/auth"))
                .withQueryParam("client_id", equalTo("sample_content_id"))
                .withQueryParam("redirect_uri", equalTo("http://localhost:9299/auth/redirect"))
                .withQueryParam("scope", equalTo("openid name"))
                .willReturn(temporaryRedirect("http://localhost:9299/auth/redirect?code=sample_code"))
        )

        wireMockRule.stubFor(
            post("/token")
                .withRequestBody(equalTo(
                    mapOf(
                        "code" to "sample_code",
                        "client_id" to "sample_content_id",
                        "client_secret" to "sample_secret_key",
                        "redirect_uri" to "http://localhost:9299/auth/redirect",
                        "grant_type" to "authorization_code"
                    ).mapValues { URLEncoder.encode(it.value, StandardCharsets.UTF_8) }
                        .entries
                        .joinToString("&")
                ))
                .willReturn(
                    jsonResponse(
                        """{
                        |"access_token": "${createAccessToken()}",
                        |"id_token": "${createIdTokenJwt()}" 
                        |}""".trimMargin(), 200
                    )
                )
        )
    }

    @Test
    fun `should open authorisation dialog`() {
        // when
        open("/")

        // then
        LeftNavPage.search.shouldBe(visible)
        WebDriverRunner.getWebDriver().manage().cookies.shouldMatchAtLeastOneOf { it.name == "auth.access_token" }
        WebDriverRunner.getWebDriver().manage().cookies.shouldMatchAtLeastOneOf { it.name == "auth.username" }
        verify(getRequestedFor(urlPathEqualTo("/auth")))
        verify(postRequestedFor(urlEqualTo("/token")))
    }

    private fun createIdTokenJwt(): String {
        return JWT.create()
            .withPayload(
                mapOf(
                    "name" to "john"
                )
            )
            .withExpiresAt(Date.from(Instant.now().plus(1, DAYS)))
            .sign(Algorithm.HMAC256("some secret"))
    }
}