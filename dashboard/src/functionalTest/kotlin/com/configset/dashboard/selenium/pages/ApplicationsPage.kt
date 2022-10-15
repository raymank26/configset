package com.configset.dashboard.selenium.pages

import com.codeborne.selenide.Selenide
import org.openqa.selenium.By

object ApplicationsPage {
    val applicationsTable = Selenide.element(By.className("applications-table"))
    val createNewApplicationButton = Selenide.element(By.className("applications-create-button"))
}