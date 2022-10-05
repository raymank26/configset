package com.configset.dashboard.pages

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Selenide.element
import com.configset.sdk.proto.ShowPropertyItem
import org.openqa.selenium.By

object SearchPage {
    val applicationNameInput = element(By.name("application-name"))
    val propertyNameInput = element(By.name("property-name"))
    val propertyValueInput = element(By.name("property-value"))
    val hostNameInput = element(By.name("hostname"))
    val searchButton = element(By.name("search"))
    val searchResultsEmpty = element("#properties-search-result-empty")
    private val searchResults = element("#properties-search-result")

    fun searchResultsShouldContainProperty(property: ShowPropertyItem) {
        searchResults.shouldHave(text(property.propertyName))
        searchResults.shouldHave(text(property.applicationName))
        searchResults.find(By.className("expand")).shouldHave(text("Expand"))
    }
}