package com.configset.dashboard.selenium

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide.open
import com.configset.dashboard.FULL_ROLES_ACCESS_TOKEN
import com.configset.dashboard.selenium.pages.ApplicationsPage
import com.configset.sdk.Application
import com.configset.sdk.ApplicationId
import org.junit.jupiter.api.Test

class ApplicationsPageTest : SeleniumTest() {

    @Test
    fun `should show applications and but not create button`() {
        // given
        authenticated()
        mockConfigServiceExt.whenListApplications()
            .answer(
                listOf(
                    Application(ApplicationId(2389L), "testApp1"),
                    Application(ApplicationId(28923L), "testApp2")
                )
            )

        // when
        open("/applications")

        // then
        ApplicationsPage.applicationsTable.shouldHave(text("testApp1"))
        ApplicationsPage.applicationsTable.shouldHave(text("testApp2"))
        ApplicationsPage.createNewApplicationButton.shouldNotBe(visible)
    }

    @Test
    fun `should create button`() {
        // given
        authenticated(FULL_ROLES_ACCESS_TOKEN)
        mockConfigServiceExt.whenListApplications()
            .answer(emptyList())

        // when
        open("/applications")

        // then
        ApplicationsPage.createNewApplicationButton.shouldBe(visible)
    }
}