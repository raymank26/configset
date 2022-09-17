package com.configset.dashboard

import com.codeborne.selenide.Configuration
import com.configset.dashboard.infra.BaseDashboardTest
import org.junit.Before

abstract class FunctionalTest : BaseDashboardTest() {

    @Before
    fun beforeUi() {
        Configuration.baseUrl = "http://localhost:9299"
    }

    fun authenticated() {
    }
}