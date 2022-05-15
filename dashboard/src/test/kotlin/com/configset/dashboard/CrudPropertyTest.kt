package com.configset.dashboard

import com.configset.sdk.proto.CreateHostResponse
import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import io.mockk.verify
import org.amshove.kluent.invoking
import org.amshove.kluent.should
import org.amshove.kluent.`should match at least one of`
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldThrow
import org.junit.Test

class CrudPropertyTest : BaseDashboardTest() {

    private val appName = "testApp"
    private val hostName = "srvd1"

    @Test
    fun `update should be indempotent`() {
        mockConfigServiceExt.whenListApplications().answer { appName }
        mockConfigServiceExt.whenListHosts().answer { listOf(hostName) }
        mockConfigServiceExt.whenUpdateProperty()
            .answer { req ->
                req.propertyName shouldBeEqualTo "someName"
                req.propertyValue shouldBeEqualTo "234"
                UpdatePropertyResponse.Type.OK
            }

        insertProperty()
        insertProperty()

        verify(exactly = 2) { mockConfigService.updateProperty(any(), any()) }
    }

    @Test
    fun `should read property`() {
        mockConfigServiceExt.whenReadProperty()
            .answer { req ->
                req.propertyName shouldBeEqualTo "propertyName"
                req.hostName shouldBeEqualTo hostName
                PropertyItem.newBuilder()
                    .setPropertyName("propertyName")
                    .setPropertyValue("value")
                    .setApplicationName(appName)
                    .build()
            }
        getProperty(appName, hostName).shouldNotBeNull()
    }

    @Test
    fun `should return empty JSON if property doesn't exist`() {
        mockConfigServiceExt.whenReadProperty()
            .answer { req ->
                req.propertyName shouldBeEqualTo "propertyName"
                req.hostName shouldBeEqualTo hostName
                null
            }
        getProperty(appName, hostName).shouldBeNull()
    }

    @Test
    fun `should throw an exception if a conflict is found`() {
        mockConfigServiceExt.whenListApplications().answer { appName }
        mockConfigServiceExt.whenListHosts().answer { listOf(hostName) }
        mockConfigServiceExt.whenUpdateProperty()
            .answer { req ->
                UpdatePropertyResponse.Type.UPDATE_CONFLICT
            }
        invoking {
            insertProperty()
        }.shouldThrow(DashboardHttpException::class)
            .exception.errorCode.shouldBeEqualTo("update.conflict")
    }

    @Test
    fun `should create a host if it's not found`() {
        mockConfigServiceExt.whenListApplications().answer { appName }
        mockConfigServiceExt.whenListHosts().answer { emptyList() }
        mockConfigServiceExt.whenCreateHost().answer {
            CreateHostResponse.newBuilder()
                .setType(CreateHostResponse.Type.OK)
                .build()
        }

        mockConfigServiceExt.whenUpdateProperty()
            .answer {
                UpdatePropertyResponse.Type.OK
            }
        insertProperty()

        verify(exactly = 1) { mockConfigService.createHost(any(), any()) }
    }

    @Test
    fun `should delete a property`() {
        mockConfigServiceExt.whenDeleteProperty()
            .answer { req ->
                req.applicationName shouldBeEqualTo appName
                req.hostName shouldBeEqualTo hostName
                req.propertyName shouldBeEqualTo "propertyName"
                req.version shouldBeEqualTo 1

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
            .answer { req ->
                updateRequests.add(req)
                UpdatePropertyResponse.Type.OK
            }

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
        invoking {
            executePostRequest("/property/import",
                mapOf(
                    Pair("applicationName", appName),
                    Pair("properties", """123""".trimIndent())),
                Map::class.java)
        }
            .shouldThrow(DashboardHttpException::class)
            .should { exception.httpCode == 400 }
            .should { exception.errorCode == "illegal.format" }
    }

    private fun insertProperty() {
        updateProperty(appName, hostName, "someName", "234", "b350bfd5-9f0b-4d3c-b2bf-ec6c429181a8")
    }
}