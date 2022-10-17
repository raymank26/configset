package com.configset.dashboard.selenium

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.WebDriverConditions
import com.configset.dashboard.FULL_ROLES_ACCESS_TOKEN
import com.configset.dashboard.selenium.pages.ApplicationsPage
import com.configset.sdk.Application
import com.configset.sdk.ApplicationId
import com.configset.sdk.proto.ApplicationDeleteRequest
import com.configset.sdk.proto.ApplicationDeletedResponse
import io.mockk.slot
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
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
    fun `should show create button`() {
        // given
        authenticated(FULL_ROLES_ACCESS_TOKEN)
        mockConfigServiceExt.whenListApplications()
            .answer(emptyList())

        // when
        open("/applications")

        // then
        ApplicationsPage.createNewApplicationButton.shouldBe(visible)
    }

    @Test
    fun `should delete property`() {
        // given
        authenticated(FULL_ROLES_ACCESS_TOKEN)
        val id = ApplicationId(238923L)
        mockConfigServiceExt.whenListApplications()
            .answer(listOf(Application(id, "test1")))

        val deleteApplicationRequest = slot<ApplicationDeleteRequest>()
        mockConfigServiceExt.whenDeleteApplication {
            capture(deleteApplicationRequest)
        }.answer(
            ApplicationDeletedResponse.newBuilder()
                .setType(ApplicationDeletedResponse.Type.OK)
                .build()
        )

        // when
        open("/applications")
        ApplicationsPage.getApplicationRow(id)
            .deleteButton.click()
        Selenide.switchTo().alert().accept()

        // then
        Selenide.webdriver().shouldHave(WebDriverConditions.urlStartingWith("$BASE_URL/applications"))
        deleteApplicationRequest.captured.also {
            it.applicationName shouldBeEqualTo "test1"
            it.requestId.shouldNotBeEmpty()
        }
    }

    @Test
    fun `should show error when application deletion failed`() {
        // given
        authenticated(FULL_ROLES_ACCESS_TOKEN)
        val id = ApplicationId(238923L)
        mockConfigServiceExt.whenListApplications()
            .answer(listOf(Application(id, "test1")))

        val deleteApplicationRequest = slot<ApplicationDeleteRequest>()
        mockConfigServiceExt.whenDeleteApplication {
            capture(deleteApplicationRequest)
        }.answer(
            ApplicationDeletedResponse.newBuilder()
                .setType(ApplicationDeletedResponse.Type.APPLICATION_NOT_FOUND)
                .build()
        )

        // when
        open("/applications")
        ApplicationsPage.getApplicationRow(id)
            .deleteButton.click()
        Selenide.switchTo().alert().accept()

        // then
        Selenide.switchTo().alert().text.shouldBeEqualTo("APPLICATION_NOT_FOUND")
    }
}