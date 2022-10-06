package com.configset.dashboard

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.WebDriverRunner
import com.configset.dashboard.infra.BaseDashboardTest
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Before
import org.openqa.selenium.Cookie

const val BASE_URL = "http://localhost:9299"

abstract class FunctionalTest : BaseDashboardTest() {

    @Before
    fun beforeUi() {
        Selenide.clearBrowserCookies()
        Configuration.proxyEnabled = true
        Configuration.baseUrl = BASE_URL
        Configuration.proxyHost = "127.0.0.1"
        Configuration.proxyPort = 39823
    }

    fun createAccessToken(): String {
        return RandomStringUtils.randomAlphabetic(16)
    }

    fun authenticated() {
        open(Configuration.baseUrl)
        WebDriverRunner.getAndCheckWebDriver().manage().addCookie(
            Cookie(
                "auth.access_token",
                createAccessToken()
            )
        )
    }
}