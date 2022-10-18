package com.configset.dashboard.selenium

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.WebDriverConditions
import com.configset.dashboard.FULL_ROLES_ACCESS_TOKEN
import com.configset.dashboard.selenium.pages.ApplicationsPage
import com.configset.dashboard.selenium.pages.UpdateApplicationPage
import com.configset.sdk.Application
import com.configset.sdk.ApplicationId
import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.sdk.proto.ApplicationCreatedResponse
import com.configset.sdk.proto.ApplicationDeleteRequest
import com.configset.sdk.proto.ApplicationDeletedResponse
import com.configset.sdk.proto.ApplicationUpdateRequest
import com.configset.sdk.proto.ApplicationUpdatedResponse
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
        ApplicationsPage.getApplicationRow(ApplicationId(2389L)).updateButton.shouldBe(visible)
        ApplicationsPage.getApplicationRow(ApplicationId(2389L)).deleteButton.shouldBe(visible)
        ApplicationsPage.applicationsTable.shouldHave(text("testApp2"))
        ApplicationsPage.getApplicationRow(ApplicationId(28923L)).updateButton.shouldBe(visible)
        ApplicationsPage.getApplicationRow(ApplicationId(28923L)).deleteButton.shouldBe(visible)

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
    fun `should delete application`() {
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

    @Test
    fun `create button should redirect to application create page`() {
        // given
        authenticated(FULL_ROLES_ACCESS_TOKEN)
        val id = ApplicationId(238923L)
        mockConfigServiceExt.whenListApplications()
            .answer(listOf(Application(id, "test1")))

        // when
        open("/applications")
        ApplicationsPage.createNewApplicationButton.click()

        // then
        Selenide.webdriver().shouldHave(WebDriverConditions.urlStartingWith("$BASE_URL/applications/create"))
    }

    @Test
    fun `should create new application`() {
        // given
        authenticated(FULL_ROLES_ACCESS_TOKEN)
        val applicationCreateRequest = slot<ApplicationCreateRequest>()
        mockConfigServiceExt.whenCreateApplication {
            capture(applicationCreateRequest)
        }.answer(ApplicationCreatedResponse.Type.OK)

        mockConfigServiceExt.whenListApplications()
            .answer(listOf())

        // when
        open("/applications/create")
        UpdateApplicationPage.applicationNameInput.value = "New app"
        UpdateApplicationPage.updateButton.click()

        // then
        Selenide.webdriver().shouldHave(WebDriverConditions.url("$BASE_URL/applications"))
        applicationCreateRequest.captured.also {
            it.applicationName shouldBeEqualTo "New app"
        }
    }

    @Test
    fun `should update application`() {
        // given
        authenticated(FULL_ROLES_ACCESS_TOKEN)
        val applicationUpdateRequest = slot<ApplicationUpdateRequest>()
        mockConfigServiceExt.whenUpdateApplication {
            capture(applicationUpdateRequest)
        }.answer(ApplicationUpdatedResponse.Type.OK)

        mockConfigServiceExt.whenListApplications()
            .answer(listOf(Application(ApplicationId(1231L), "Some-app")))

        // when
        open("/applications/update?applicationName=Some-app")
        UpdateApplicationPage.applicationNameInput.value = "New app"
        UpdateApplicationPage.updateButton.click()

        // then
        Selenide.webdriver().shouldHave(WebDriverConditions.url("$BASE_URL/applications"))
        applicationUpdateRequest.captured.also {
            it.id shouldBeEqualTo ApplicationId(1231L).id.toString()
            it.applicationName shouldBeEqualTo "New app"
        }
    }
}