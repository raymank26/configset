package com.letsconfig.dashboard

import org.junit.Rule
import org.junit.Test

class UpdatePropertyTest {
    @Rule
    @JvmField
    val dashboardRule = DashboardRule()

    @Test
    fun test() {
        dashboardRule.executePostRequest("/application/", mapOf(Pair("appName", "testApp")), Any::class.java)
        val hostName = "srvd1"
        dashboardRule.executePostRequest("/property/update", mapOf(
                Pair("applicationName", "testApp"),
                Pair("hostName", hostName),
                Pair("propertyName", "propertyName"),
                Pair("propertyValue", "234")
        ), Map::class.java)
    }
}
