package com.configset.dashboard.selenium.pages

import com.codeborne.selenide.Selenide
import com.configset.sdk.ApplicationId
import org.openqa.selenium.By

object ApplicationsPage {
    val applicationsTable = Selenide.element(By.className("applications-table"))
    val createNewApplicationButton = Selenide.element(By.className("applications-create-button"))

    fun getApplicationRow(id: ApplicationId): ApplicationRow = ApplicationRow(id)
}

class ApplicationRow(id: ApplicationId) {
    val updateButton = Selenide.element(By.className("application-update-button-${id.id}"))
    val deleteButton = Selenide.element(By.className("application-delete-button-${id.id}"))
}