package com.configset.dashboard

import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide.open
import com.configset.dashboard.pages.LeftNavPage
import com.configset.dashboard.pages.MainPage
import org.junit.Test

class MainPageTest : FunctionalTest() {

    @Test
    fun `should have main elements`() {
        // given
        authenticated()

        // when
        open("/")

        // then
        MainPage.applicationNameInput.shouldBe(visible)
        MainPage.propertyNameInput.shouldBe(visible)
        MainPage.propertyValueInput.shouldBe(visible)
        MainPage.searchButton.shouldBe(visible)
        LeftNavPage.search.shouldBe(visible)
        LeftNavPage.hosts.shouldBe(visible)
    }
}