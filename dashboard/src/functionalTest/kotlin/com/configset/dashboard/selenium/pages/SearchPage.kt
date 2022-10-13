package com.configset.dashboard.selenium.pages

import com.codeborne.selenide.CollectionCondition
import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement
import com.configset.sdk.proto.PropertyItem
import org.openqa.selenium.By

object SearchPage {
    val applicationNameInput = element(By.name("applicationName"))
    val propertyNameInput = element(By.name("propertyName"))
    val propertyValueInput = element(By.name("propertyValue"))
    val hostNameInput = element(By.name("hostName"))
    val searchButton = element(By.className("search-properties-button"))
    val searchResultsEmpty = element("#properties-search-result-empty")
    val createPropertyLink = element(By.className("create-property"))
    private val searchResults = element("#properties-search-result")

    fun searchResultsShouldContainProperty(property: PropertyItem) {
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

    fun getUpdateButton(): SelenideElement {
        return selenideElement.find(By.className("update-property")).apply {
            shouldBe(visible)
        }
    }

    fun getDeleteButton(): SelenideElement {
        return selenideElement.find(By.className("delete-property")).apply {
            shouldBe(visible)
        }
    }
}