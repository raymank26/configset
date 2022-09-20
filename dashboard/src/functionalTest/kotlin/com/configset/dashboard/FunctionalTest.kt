package com.configset.dashboard

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.WebDriverRunner
import com.configset.dashboard.infra.BaseDashboardTest
import org.junit.Before
import org.openqa.selenium.Cookie
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

abstract class FunctionalTest : BaseDashboardTest() {

    @Before
    fun beforeUi() {
        Selenide.clearBrowserCookies()
        Configuration.proxyEnabled = true
        Configuration.baseUrl = "http://localhost:9299"
        Configuration.proxyHost = "127.0.0.1"
        Configuration.proxyPort = 39823
    }

    fun createAccessTokenJwt(): String {
        return JWT.create().withPayload(
            mapOf(
                "token" to "content"
            )
        )
            .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
            .sign(Algorithm.HMAC256("some secret"))
    }

    fun authenticated() {
        open(Configuration.baseUrl)
        WebDriverRunner.getAndCheckWebDriver().manage().addCookie(
            Cookie(
                "auth.access_token",
                createAccessTokenJwt()
            )
        )
    }
}