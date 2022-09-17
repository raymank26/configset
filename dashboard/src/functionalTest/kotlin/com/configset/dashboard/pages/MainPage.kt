package com.configset.dashboard.pages

import com.codeborne.selenide.Selenide.element
import org.openqa.selenium.By

object MainPage {
    val applicationNameInput = element(By.name("application-name"))
    val propertyNameInput = element(By.name("property-name"))
    val propertyValueInput = element(By.name("property-value"))
    val searchButton = element(By.name("search"))
}