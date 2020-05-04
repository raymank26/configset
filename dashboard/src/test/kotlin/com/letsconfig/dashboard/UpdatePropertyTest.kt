package com.letsconfig.dashboard

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
    }

    @Test
    fun testIdempotentUpdate() {
        val update = {
            dashboardRule.executePostRequest("/property/update", mapOf(
                    Pair("applicationName", "testApp"),
                    Pair("hostName", "srvd1"),
                    Pair("propertyName", "propertyName"),
                    Pair("propertyValue", "234")
            ), Map::class.java, requestId = "b350bfd5-9f0b-4d3c-b2bf-ec6c429181a8")
        }
        update.invoke()
        update.invoke()
    }
}