package com.configset.dashboard.selenium.pages

import com.codeborne.selenide.Selenide
import org.openqa.selenium.By

object UpdatePropertyPage {
    val applicationNameInput = Selenide.element(By.name("applicationName"))
    val hostNameInput = Selenide.element(By.name("hostName"))
    val propertyNameInput = Selenide.element(By.name("propertyName"))
    val propertyValueInput = Selenide.element(By.name("propertyValue"))

    val errorContainer = Selenide.element(By.className("property-update-error"))

    val updateButton = Selenide.element(By.className("property-update-button"))
}
