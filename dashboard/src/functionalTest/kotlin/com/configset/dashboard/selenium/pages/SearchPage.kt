package com.configset.dashboard.selenium.pages

import com.codeborne.selenide.CollectionCondition
import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement
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

    fun findSearchResultRow(appName: String, propertyName: String): PropertyResultRow {
        val searchRows = searchResults.findAll(By.className("property-item-row"))
        searchRows.shouldBe(CollectionCondition.sizeGreaterThan(0))
        for (elem in searchRows.asFixedIterable()) {
            if (elem.has(text(propertyName)) && elem.has(text(appName))) {
                return PropertyResultRow(elem)
            }
        }
        error("Cannot find search result row for propertyName = $propertyName and appName = $appName")
    }
}

class PropertyResultRow(private val selenideElement: SelenideElement) {

    fun getExpandButton(): SelenideElement {
        return selenideElement.find(By.className("expand")).apply {
            shouldBe(visible)
        }
    }

    fun getPropertyItem(hostname: String, propertyValue: String): PropertyItemRow {
        val rows = selenideElement.findAll(By.tagName("tr"))
        rows.should(CollectionCondition.sizeGreaterThan(0))
        return rows.find { it.has(text(hostname)) && it.has(text(propertyValue)) }
            ?.let { PropertyItemRow(it) }
            ?: error("Cannot find property item row by hostname = $hostname and propertyValue = $propertyValue")
    }
}

class PropertyItemRow(private val selenideElement: SelenideElement) {

    fun getEditButton(): SelenideElement {
        return selenideElement.find(By.className("edit")).apply {
            shouldBe(visible)
        }
    }
}