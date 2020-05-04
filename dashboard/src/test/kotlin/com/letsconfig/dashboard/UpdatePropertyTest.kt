package com.letsconfig.dashboard

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UpdatePropertyTest {

    @Rule
    @JvmField
    val dashboardRule = DashboardRule()

    @Before
    fun before() {
        dashboardRule.executePostRequest("/application/", mapOf(Pair("appName", "testApp")), Any::class.java)
        dashboardRule.executePostRequest("/application/", mapOf(Pair("appName", "testApp2")), Any::class.java)
        dashboardRule.executePostRequest("/property/update", mapOf(
                Pair("applicationName", "testApp"),
                Pair("hostName", "srvd1"),
                Pair("propertyName", "propertyName"),
                Pair("propertyValue", "234")
        ), Map::class.java)

        dashboardRule.executePostRequest("/property/update", mapOf(
                Pair("applicationName", "testApp2"),
                Pair("hostName", "srvd1"),
                Pair("propertyName", "propertyName2"),
                Pair("propertyValue", "234")
        ), Map::class.java)
    }

    @Test
    fun testUpdateList() {
        val res = dashboardRule.executeGetRequest("/property/list?applicationName=testApp", List::class.java)
        res.size shouldBeEqualTo 1
    }

    @Test
    fun testSearch() {
        val byHost = dashboardRule.executeGetRequest("/property/search?hostName=srvd", Map::class.java)
        byHost.size shouldBeEqualTo 2

        val byPropertyName = dashboardRule.executeGetRequest("/property/search?propertyName=prop", Map::class.java)
        byPropertyName.size shouldBeEqualTo 2

        val byPropertyValue = dashboardRule.executeGetRequest("/property/search?propertyValue=234", Map::class.java)
        byPropertyValue.size shouldBeEqualTo 2
    }
}
