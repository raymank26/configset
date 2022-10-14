package com.configset.dashboard.selenium

import com.codeborne.selenide.Condition.href
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.Selenide.webdriver
import com.codeborne.selenide.WebDriverConditions
import com.configset.dashboard.selenium.pages.LeftNavPage
import com.configset.dashboard.selenium.pages.SearchPage
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.SearchPropertiesRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SearchPageTest : SeleniumTest() {

    @BeforeEach
    fun beforeSearchPage() {
        authenticated()
    }

    @Test
    fun `should redirect to properties`() {
        // when
        open("/")

        // then
        webdriver().shouldHave(WebDriverConditions.urlStartingWith("$BASE_URL/properties"))
    }

    @Test
    fun `should have main elements`() {
        // when
        open("/properties")

        // then
        SearchPage.applicationNameInput.shouldBe(visible)
        SearchPage.propertyNameInput.shouldBe(visible)
        SearchPage.propertyValueInput.shouldBe(visible)
        SearchPage.hostNameInput.shouldBe(visible)
        SearchPage.searchButton.shouldBe(visible)
        SearchPage.createPropertyLink.shouldBe(visible)

        LeftNavPage.properties.apply {
            shouldHave(href("/properties"))
            shouldBe(visible)
        }
        LeftNavPage.applications.apply {
            shouldHave(href("/applications"))
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
        open("/properties")
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
        open("/properties")
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
        open("/properties")
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
        open("/properties")
        SearchPage.applicationNameInput.value = "Sample app"
        SearchPage.searchButton.click()
        val rowElement = SearchPage.findSearchResultRow(properties[0].applicationName, properties[0].propertyName)
        rowElement.getExpandButton().click()
        rowElement.getPropertyItem("sample host", "bar").getUpdateButton().click()

        // then
        webdriver().shouldHave(WebDriverConditions.urlStartingWith("$BASE_URL/properties/update"))
    }

    @Test
    fun `click on add property should redirect to a separate page`() {
        // given
        authenticated()

        // when
        SearchPage.createPropertyLink.click()

        // then
        webdriver().shouldHave(WebDriverConditions.urlStartingWith("$BASE_URL/properties/create"))
    }
}