package com.letsconfig.dashboard

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Rule
import org.junit.Test

class UpdatePropertyTest {

    @Rule
    @JvmField
    val dashboardRule = DashboardRule()

    @Test
    fun testUpdateList() {
        dashboardRule.executePostRequest("/application/", mapOf(Pair("appName", "testApp")), Any::class.java)
        dashboardRule.executePostRequest("/property/update", mapOf(
                Pair("applicationName", "testApp"),
                Pair("hostName", "srvd1"),
                Pair("propertyName", "propertyName"),
                Pair("propertyValue", "234")
        ), Map::class.java)

        dashboardRule.executePostRequest("/property/update", mapOf(
                Pair("applicationName", "testApp"),
                Pair("hostName", "srvd1"),
                Pair("propertyName", "propertyName2"),
                Pair("propertyValue", "234")
        ), Map::class.java)

        val res = dashboardRule.executeGetRequest("/property/list?appName=testApp", List::class.java)
        res.size shouldBeEqualTo 2
    }
}
