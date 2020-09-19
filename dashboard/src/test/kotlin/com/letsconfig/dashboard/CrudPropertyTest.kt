package com.configset.dashboard

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CrudPropertyTest {

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
            insertProperty()
        }
        update.invoke()
        update.invoke()
    }

    @Test
    fun testReadProperty() {
        insertProperty()
        dashboardRule.executeGetRequest("/property/get", Map::class.java, mapOf(
                Pair("applicationName", "testApp"),
                Pair("hostName", "srvd1"),
                Pair("propertyName", "propertyName")
        )).size shouldBeGreaterThan 0
    }

    @Test
    fun testDelete() {
        insertProperty()

        dashboardRule.executePostRequest("/property/delete", mapOf(
                Pair("applicationName", "testApp"),
                Pair("hostName", "srvd1"),
                Pair("propertyName", "propertyName"),
                Pair("version", "1")
        ), Map::class.java, requestId = "1239")
    }

    private fun insertProperty(): Map<*, *>? {
        return dashboardRule.executePostRequest("/property/update", mapOf(
                Pair("applicationName", "testApp"),
                Pair("hostName", "srvd1"),
                Pair("propertyName", "propertyName"),
                Pair("propertyValue", "234")
        ), Map::class.java, requestId = "b350bfd5-9f0b-4d3c-b2bf-ec6c429181a8")
    }

    @Test
    fun testIdempotentUpdateDifferentRequests() {
        dashboardRule.updateProperty(
                applicationName = "testApp",
                hostName = "srvd1",
                propertyName = "propertyName",
                propertyValue = "234",
                requestId = "b350bfd5-9f0b-4d3c-b2bf-ec6c429181a8"
        )

        dashboardRule.updateProperty(
                applicationName = "testApp",
                hostName = "srvd1",
                propertyName = "propertyName",
                propertyValue = "235",
                requestId = "b350bfd5-9f0b-4d3c-b2bf-ec6c429181a8"
        )
    }

    @Test
    fun testImportProperties() {
        dashboardRule.updateProperty(applicationName = "testApp", hostName = "srvd1", propertyName = "foobar",
                propertyValue = "val", requestId = "1238913")

        dashboardRule.executePostRequest("/property/import", mapOf(
                Pair("applicationName", "testApp"),
                Pair("properties", """
                    <properties>
                        <property>
                            <host>srvd2</host>
                            <name>spam</name>
                            <value>baz</value>
                        </property>
                        <property>
                            <host>srvd1</host>
                            <name>foobar</name>
                            <value>baz</value>
                        </property>
                    </properties>
                """.trimIndent())
        ), Map::class.java)
        val searchResult = dashboardRule.searchProperties(applicationName = "testApp")
        searchResult.size shouldBeEqualTo 2

        searchResult.first { it.propertyName == "spam" }.run {
            applicationName shouldBeEqualTo "testApp"
            hostName shouldBeEqualTo "srvd2"
            propertyValue shouldBeEqualTo "baz"
        }

        searchResult.first { it.propertyName == "foobar" }.run {
            applicationName shouldBeEqualTo "testApp"
            hostName shouldBeEqualTo "srvd1"
            propertyValue shouldBeEqualTo "baz"
        }
    }

    @Test
    fun testImportIllegalFormat() {
        @Suppress("UNCHECKED_CAST")
        val response: Map<String, Any> = dashboardRule.executePostRequest("/property/import", mapOf(
                Pair("applicationName", "testApp"),
                Pair("properties", """123""".trimIndent())), Map::class.java, expectedResponseCode = 400) as Map<String, Any>
        response.getValue("code") shouldBeEqualTo "illegal.format"
        println(response)
    }
}