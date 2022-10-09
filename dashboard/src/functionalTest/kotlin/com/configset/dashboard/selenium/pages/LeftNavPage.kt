package com.configset.dashboard.selenium.pages

import com.codeborne.selenide.Selenide.element

object LeftNavPage {
    val search = element("#search-page")
    val hosts = element("#hosts-page")
}