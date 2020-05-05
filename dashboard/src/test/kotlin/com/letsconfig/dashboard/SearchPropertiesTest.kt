package com.letsconfig.dashboard

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SearchPropertiesTest {

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
        dashboardRule.searchProperties(applicationName = "testApp").size shouldBeEqualTo 1
    }

    @Test
    fun testSearch() {
        dashboardRule.searchProperties(hostName = "srvd").size shouldBeEqualTo 2
        dashboardRule.searchProperties(propertyName = "prop").size shouldBeEqualTo 2
        dashboardRule.searchProperties(propertyValue = "234").size shouldBeEqualTo 2
    }
}
