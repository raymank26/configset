package com.configset.dashboard

import arrow.core.Either
import com.configset.dashboard.infra.BaseDashboardTest
import com.configset.dashboard.infra.DashboardHttpFailure
import com.configset.dashboard.infra.expectLeft
import com.configset.dashboard.infra.expectRight
import com.configset.sdk.proto.CreateHostResponse
import com.configset.sdk.proto.DeletePropertyRequest
import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.ReadPropertyRequest
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import io.mockk.verify
import org.amshove.kluent.any
import org.amshove.kluent.should
import org.amshove.kluent.`should match at least one of`
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.Test

class CrudPropertyTest : BaseDashboardTest() {

    private val appName = "testApp"
    private val hostName = "srvd1"

    @Test
    fun `update should be idempotent`() {
        mockConfigServiceExt.whenListApplications().answer(appName)
        mockConfigServiceExt.whenListHosts().answer(listOf(hostName))
        mockConfigServiceExt.whenUpdateProperty(
            UpdatePropertyRequest.newBuilder()
                .setPropertyName("someName")
                .setPropertyValue("234")
                .build()
        )
            .answer(UpdatePropertyResponse.Type.OK)

        insertProperty().expectRight()
        insertProperty().expectRight()

        verify(exactly = 2) { mockConfigService.updateProperty(any(), any()) }
    }

    @Test
    fun `should read property`() {
        mockConfigServiceExt.whenReadProperty(
            ReadPropertyRequest.newBuilder()
                .setPropertyName("propertyName")
                .setHostName(hostName)
                .build()
        )
            .answer(
                PropertyItem.newBuilder()
                    .setPropertyName("propertyName")
                    .setPropertyValue("value")
                    .setApplicationName(appName)
                    .build()
            )
        dashboardClient.getProperty(appName, hostName)
            .expectRight()
            .shouldNotBeNull()
    }

    @Test
    fun `should return empty JSON if property doesn't exist`() {
        mockConfigServiceExt.whenReadProperty(
            ReadPropertyRequest.newBuilder()
                .setPropertyName("propertyName")
                .setHostName(hostName)
                .build()
        )
            .answer(null)
        dashboardClient.getProperty(appName, hostName)
            .expectRight()
            .shouldBeNull()
    }

    @Test
    fun `should throw an exception if a conflict is found`() {
        mockConfigServiceExt.whenListApplications().answer(appName)
        mockConfigServiceExt.whenListHosts().answer(listOf(hostName))
        mockConfigServiceExt.whenUpdateProperty(any())
            .answer(UpdatePropertyResponse.Type.UPDATE_CONFLICT)
        val err = insertProperty()
            .expectLeft()
        err.errorCode shouldBeEqualTo "update.conflict"
    }

    @Test
    fun `should create a host if it's not found`() {
        mockConfigServiceExt.whenListApplications().answer(appName)
        mockConfigServiceExt.whenListHosts().answer(emptyList())
        mockConfigServiceExt.whenCreateHost(any()).answer(
            CreateHostResponse.newBuilder()
                .setType(CreateHostResponse.Type.OK)
                .build()
        )

        mockConfigServiceExt.whenUpdateProperty(any())
            .answer(UpdatePropertyResponse.Type.OK)
        insertProperty()

        verify(exactly = 1) { mockConfigService.createHost(any(), any()) }
    }

    @Test
    fun `should delete a property`() {
        mockConfigServiceExt.whenDeleteProperty(
            DeletePropertyRequest.newBuilder()
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName("some property")
                .setVersion(1)
                .build()
        )
            .answer(
                DeletePropertyResponse.newBuilder()
                    .setType(DeletePropertyResponse.Type.OK)
                    .build()
            )
        dashboardClient.deleteProperty(appName, hostName, "some property")
            .expectRight()
    }

    @Test
    fun testImportProperties() {
        mockConfigServiceExt.whenListApplications()
            .answer(appName)

        mockConfigServiceExt.whenListHosts()
            .answer(listOf("srvd2", "srvd1"))
        mockConfigServiceExt.whenReadProperty(any())
            .answer(null)

        val updateRequests = mutableListOf<UpdatePropertyRequest>()
        mockConfigServiceExt.whenUpdateProperty(any())
            .answer(UpdatePropertyResponse.Type.OK)

        dashboardClient.importProperties(
            appName,
            """
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
            """.trimIndent()
        ).expectRight()

        updateRequests `should match at least one of` {
            it.hostName == "srvd2"
                    && it.propertyName == "spam"
                    && it.propertyValue == "baz"
        }
    }

    @Test
    fun testImportIllegalFormat() {
        dashboardClient.importProperties(appName, "illegal input")
            .expectLeft()
            .should { httpCode == 400 }
            .should { errorCode == "illegal.format" }
    }

    private fun insertProperty(): Either<DashboardHttpFailure, Unit> {
        return dashboardClient.updateProperty(appName,
            hostName,
            "someName",
            "234",
            "b350bfd5-9f0b-4d3c-b2bf-ec6c429181a8")
    }
}