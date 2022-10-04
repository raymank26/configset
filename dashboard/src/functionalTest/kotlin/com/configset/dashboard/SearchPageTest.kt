package com.configset.dashboard

import com.codeborne.selenide.Condition.href
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide.open
import com.configset.dashboard.pages.LeftNavPage
import com.configset.dashboard.pages.SearchPage
import com.configset.sdk.proto.SearchPropertiesRequest
import com.configset.sdk.proto.ShowPropertyItem
import org.junit.Before
import org.junit.Test

class SearchPageTest : FunctionalTest() {

    @Before
    fun foreSearchPage() {
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
            ShowPropertyItem.newBuilder()
                .setApplicationName("Sample app")
                .setPropertyName("Foo")
                .setPropertyValue("bar")
                .setHostName("sample host")
                .setVersion(2)
                .build(),
            ShowPropertyItem.newBuilder()
                .setApplicationName("Sample app")
                .setPropertyName("Next property")
                .setPropertyValue("Next property value")
                .setHostName("Some host name")
                .setVersion(2)
                .build(),

            )
        mockConfigServiceExt.whenSearchProperties(request)
            .answer(properties)

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
        mockConfigServiceExt.whenSearchProperties(request)
            .answer(emptyList())

        // when
        open("/")
        SearchPage.applicationNameInput.value = "Sample app"
        SearchPage.searchButton.click()

        // then
        SearchPage.searchResultsEmpty.shouldBe(visible)
    }
}