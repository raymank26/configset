package com.configset.dashboard.selenium

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.WebDriverRunner
import com.configset.dashboard.FunctionalTest
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.BeforeEach
import org.openqa.selenium.Cookie

const val BASE_URL = "http://localhost:9299"

abstract class SeleniumTest : FunctionalTest() {

    @BeforeEach
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