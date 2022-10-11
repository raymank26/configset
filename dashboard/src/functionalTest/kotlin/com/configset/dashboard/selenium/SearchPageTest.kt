package com.configset.dashboard.selenium

import com.codeborne.selenide.Condition.href
import com.codeborne.selenide.Condition.value
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.Selenide.webdriver
import com.codeborne.selenide.WebDriverConditions
import com.configset.dashboard.selenium.pages.LeftNavPage
import com.configset.dashboard.selenium.pages.SearchPage
import com.configset.dashboard.selenium.pages.UpdatePropertyPage
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.ReadPropertyRequest
import com.configset.sdk.proto.SearchPropertiesRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import io.mockk.slot
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SearchPageTest : SeleniumTest() {

    @BeforeEach
    fun beforeSearchPage() {
        authenticated()
    }

    @Test
    fun `should have main elements`() {
        // when
        open("/")

        // then
        SearchPage.applicationNameInput.shouldBe(visible)
        SearchPage.propertyNameInput.shouldBe(visible)
        SearchPage.propertyValueInput.shouldBe(visible)
        SearchPage.hostNameInput.shouldBe(visible)
        SearchPage.searchButton.shouldBe(visible)

        LeftNavPage.search.apply {
            shouldHave(href("/"))
            shouldBe(visible)
        }
        LeftNavPage.hosts.apply {
            shouldHave(href("/hosts"))
            shouldBe(visible)
        }
    }

    @Test
    fun `should search for properties by app name`() {
        // given
        val request = SearchPropertiesRequest.newBuilder()
            .setApplicationName("Sample app")
            .setPropertyName("")
            .setHostName("")
            .setPropertyValue("")
            .build()
        val properties = listOf(
            PropertyItem.newBuilder()
                .setApplicationName("Sample app")
                .setPropertyName("Foo")
                .setPropertyValue("bar")
                .setHostName("sample host")
                .setVersion(2)
                .build(),
            PropertyItem.newBuilder()
                .setApplicationName("Sample app")
                .setPropertyName("Next property")
                .setPropertyValue("Next property value")
                .setHostName("Some host name")
                .setVersion(2)
                .build(),

            )
        mockConfigServiceExt.whenSearchProperties {
            eq(request)
        }.answer(properties)

        // when
        open("/")
        SearchPage.applicationNameInput.value = "Sample app"
        SearchPage.searchButton.click()

        // then
        SearchPage.searchResultsShouldContainProperty(properties[0])
        SearchPage.searchResultsShouldContainProperty(properties[1])
    }

    @Test
    fun `should return empty results`() {
        // given
        val request = SearchPropertiesRequest.newBuilder()
            .setApplicationName("Sample app")
            .setPropertyName("")
            .setHostName("")
            .setPropertyValue("")
            .build()
        mockConfigServiceExt.whenSearchProperties {
            eq(request)
        }.answer(emptyList())

        // when
        open("/")
        SearchPage.applicationNameInput.value = "Sample app"
        SearchPage.searchButton.click()

        // then
        SearchPage.searchResultsEmpty.shouldBe(visible)
    }

    @Test
    fun `should expand table item to see Edit button`() {
        // given
        val request = SearchPropertiesRequest.newBuilder()
            .setApplicationName("Sample app")
            .setPropertyName("")
            .setHostName("")
            .setPropertyValue("")
            .build()
        val properties = listOf(
            PropertyItem.newBuilder()
                .setApplicationName("Sample app")
                .setPropertyName("Foo")
                .setPropertyValue("bar")
                .setHostName("sample host")
                .setVersion(2)
                .build(),
        )
        mockConfigServiceExt.whenSearchProperties {
            eq(request)
        }.answer(properties)

        // when
        open("/")
        SearchPage.applicationNameInput.value = "Sample app"
        SearchPage.searchButton.click()
        val rowElement = SearchPage.findSearchResultRow(properties[0].applicationName, properties[0].propertyName)
        rowElement.getExpandButton().click()

        // then
        val foundItem = rowElement.getPropertyItem("sample host", "bar")
        foundItem.getUpdateButton().exists()
    }

    @Test
    fun `should redirect to edit page`() {
        // given
        val request = SearchPropertiesRequest.newBuilder()
            .setApplicationName("Sample app")
            .setPropertyName("")
            .setHostName("")
            .setPropertyValue("")
            .build()
        val properties = listOf(
            PropertyItem.newBuilder()
                .setApplicationName("Sample app")
                .setPropertyName("Foo")
                .setPropertyValue("bar")
                .setHostName("sample host")
                .setVersion(2)
                .build(),
        )
        mockConfigServiceExt.whenSearchProperties {
            eq(request)
        }.answer(properties)
        mockConfigServiceExt.whenReadProperty {
            any()
        }.answer(
            PropertyItem.newBuilder()
                .setApplicationName("Sample app")
                .setPropertyName("Foo")
                .setPropertyValue("bar")
                .setHostName("sample host")
                .setVersion(2)
                .build()
        )

        // when
        open("/")
        SearchPage.applicationNameInput.value = "Sample app"
        SearchPage.searchButton.click()
        val rowElement = SearchPage.findSearchResultRow(properties[0].applicationName, properties[0].propertyName)
        rowElement.getExpandButton().click()
        rowElement.getPropertyItem("sample host", "bar").getUpdateButton().click()

        // then
        webdriver().shouldHave(WebDriverConditions.urlStartingWith("$BASE_URL/update"))
    }

    @Test
    fun `should fill property form`() {
        // given
        val propertyReadRequestSlot = slot<ReadPropertyRequest>()
        mockConfigServiceExt.whenReadProperty {
            capture(propertyReadRequestSlot)
        }.answer(
            PropertyItem.newBuilder()
                .setApplicationName("sampleApp")
                .setPropertyName("Foo")
                .setPropertyValue("bar")
                .setHostName("sampleHost")
                .setVersion(2)
                .build()
        )

        // when
        open("/update?applicationName=sampleApp&propertyName=Foo&hostName=sampleHost")

        // then
        UpdatePropertyPage.applicationNameInput.apply {
            shouldBe(visible)
            shouldHave(value("sampleApp"))
        }
        UpdatePropertyPage.hostNameInput.apply {
            shouldBe(visible)
            shouldHave(value("sampleHost"))
        }
        UpdatePropertyPage.propertyNameInput.apply {
            shouldBe(visible)
            shouldHave(value("Foo"))
        }
        UpdatePropertyPage.propertyValueInput.apply {
            shouldBe(visible)
            shouldHave(value("bar"))
        }
        propertyReadRequestSlot.captured.also {
            it.applicationName shouldBeEqualTo "sampleApp"
            it.propertyName shouldBeEqualTo "Foo"
            it.hostName shouldBeEqualTo "sampleHost"
        }
    }

    @Test
    fun `should update property and redirect back`() {
        // given
        mockConfigServiceExt.whenReadProperty { any() }
            .answer(
                PropertyItem.newBuilder()
                    .setApplicationName("sampleApp")
                    .setPropertyName("Foo")
                    .setPropertyValue("bar")
                    .setHostName("sampleHost")
                    .setVersion(2)
                    .build()
            )
        mockConfigServiceExt.whenUpdateProperty { any() }
            .answer(UpdatePropertyResponse.Type.OK)

        // when
        open("/update?applicationName=sampleApp&propertyName=Foo&hostName=sampleHost")
        UpdatePropertyPage.propertyValueInput.value = "new value"
        UpdatePropertyPage.updateButton.click()

        // then
        SearchPage.searchButton.shouldBe(visible)
    }

    @Test
    fun `should show error on property update`() {
        // given
        mockConfigServiceExt.whenReadProperty { any() }
            .answer(
                PropertyItem.newBuilder()
                    .setApplicationName("sampleApp")
                    .setPropertyName("Foo")
                    .setPropertyValue("bar")
                    .setHostName("sampleHost")
                    .setVersion(2)
                    .build()
            )
        mockConfigServiceExt.whenUpdateProperty { any() }
            .answer(UpdatePropertyResponse.Type.UPDATE_CONFLICT)

        // when
        open("/update?applicationName=sampleApp&propertyName=Foo&hostName=sampleHost")
        UpdatePropertyPage.propertyValueInput.value = "new value"
        UpdatePropertyPage.updateButton.click()

        // then
        UpdatePropertyPage.errorContainer.shouldBe(visible)
    }
}