package com.letsconfig.dashboard

import org.amshove.kluent.shouldBe
import org.junit.Rule
import org.junit.Test

class CreateApplicationTest {

    @Rule
    @JvmField
    val dashboardRule = DashboardRule()

    @Test
    fun testNoApplications() {
        val res = dashboardRule.executeGetRequest("/application/list", List::class.java)
        res.isEmpty() shouldBe true
    }

    @Test
    fun createApplication() {
        dashboardRule.executePostRequest("/application/", mapOf(Pair("appName", "testApp")), Any::class.java)
    }
}