package com.configset.dashboard

import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import io.mockk.verify
import org.amshove.kluent.`should match at least one of`
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.Test

class CrudPropertyTest : BaseDashboardTest() {

    private val appName = "testApp"
    private val hostName = "srvd1"

    @Test
    fun testIdempotentUpdate() {
        mockConfigServiceExt.whenListApplications().answer { appName }
        mockConfigServiceExt.whenListHosts().answer { listOf(hostName) }
        mockConfigServiceExt.whenUpdateProperty()
            .intercept { request ->
                request.propertyName shouldBeEqualTo "someName"
                request.propertyValue shouldBeEqualTo "234"
            }.answer { UpdatePropertyResponse.Type.OK }

        val update = {
            insertProperty()
        }
        update.invoke()
        update.invoke()

        verify(exactly = 2) { mockConfigService.updateProperty(any(), any()) }
    }

    @Test
    fun testReadProperty() {
        mockConfigServiceExt.whenReadProperty()
            .intercept { req ->
                req.propertyName shouldBeEqualTo "propertyName"
                req.hostName shouldBeEqualTo hostName
            }.answer {
                PropertyItem.newBuilder()
                    .setPropertyName("propertyName")
                    .setPropertyValue("value")
                    .setApplicationName(appName)
                    .build()
            }
        executeGetRequest("/property/get", Map::class.java, mapOf(
            Pair("applicationName", appName),
            Pair("hostName", hostName),
            Pair("propertyName", "propertyName")
        )).size shouldBeGreaterThan 0
    }

    @Test
    fun testDelete() {
        mockConfigServiceExt.whenDeleteProperty()
            .intercept { req ->
                req.applicationName shouldBeEqualTo appName
                req.hostName shouldBeEqualTo hostName
                req.propertyName shouldBeEqualTo "propertyName"
                req.version shouldBeEqualTo 1
            }
            .answer {
                DeletePropertyResponse.newBuilder()
                    .setType(DeletePropertyResponse.Type.OK)
                    .build()
            }
        executePostRequest("/property/delete", mapOf(
            Pair("applicationName", appName),
            Pair("hostName", hostName),
            Pair("propertyName", "propertyName"),
            Pair("version", "1")
        ), Map::class.java, requestId = "1239")
    }

    private fun insertProperty() {
        updateProperty(appName, hostName, "someName", "234", "b350bfd5-9f0b-4d3c-b2bf-ec6c429181a8")
    }

    @Test
    fun testImportProperties() {
        mockConfigServiceExt.whenListApplications()
            .answer { appName }

        mockConfigServiceExt.whenListHosts()
            .answer { listOf("srvd2", "srvd1") }
        mockConfigServiceExt.whenReadProperty()
            .answer { null }

        val updateRequests = mutableListOf<UpdatePropertyRequest>()
        mockConfigServiceExt.whenUpdateProperty()
            .intercept {
                updateRequests.add(it)
            }.answer { UpdatePropertyResponse.Type.OK }

        executePostRequest("/property/import", mapOf(
            Pair("applicationName", appName),
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

        updateRequests.size shouldBeEqualTo 2

        updateRequests `should match at least one of` {
            it.hostName == "srvd2"
                    && it.propertyName == "spam"
                    && it.propertyValue == "baz"
        }
    }

    @Test
    fun testImportIllegalFormat() {
        @Suppress("UNCHECKED_CAST")
        val response: Map<String, Any> = executePostRequest("/property/import",
            mapOf(
                Pair("applicationName", appName),
                Pair("properties", """123""".trimIndent())),
            Map::class.java,
            expectedResponseCode = 400) as Map<String, Any>
        response.getValue("code") shouldBeEqualTo "illegal.format"
    }
}