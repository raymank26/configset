package com.configset.dashboard

import com.codeborne.selenide.AuthenticationType
import com.codeborne.selenide.BearerTokenCredentials
import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide.open
import com.configset.dashboard.infra.BaseDashboardTest
import org.junit.Before

abstract class FunctionalTest : BaseDashboardTest() {

    @Before
    fun beforeUi() {
        Configuration.proxyEnabled = true
        Configuration.baseUrl = "http://localhost:9299"
        Configuration.proxyHost = "127.0.0.1"
        Configuration.proxyPort = 39823
    }

    fun openAuthenticated(url: String) {
        open(url, AuthenticationType.BEARER, BearerTokenCredentials("some auth token"))
    }
}