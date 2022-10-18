package com.configset.dashboard.selenium.pages

import com.codeborne.selenide.Selenide
import org.openqa.selenium.By

object UpdateApplicationPage {
    val applicationNameInput = Selenide.element(By.name("applicationName"))
    val updateButton = Selenide.element(By.className("application-update-button"))
}