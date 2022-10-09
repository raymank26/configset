package com.configset.dashboard.headless

import arrow.core.Either
import com.configset.dashboard.DashboardHttpFailure
import com.configset.dashboard.FunctionalTest
import com.configset.dashboard.expectLeft
import com.configset.dashboard.expectRight
import com.configset.sdk.proto.CreateHostResponse
import com.configset.sdk.proto.DeletePropertyRequest
import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.ReadPropertyRequest
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.should
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class CrudPropertyTest : FunctionalTest() {

    private val appName = "testApp"
    private val hostName = "srvd1"

    @Test
    fun `update should be idempotent`() {
        // given
        val updateSlot = slot<UpdatePropertyRequest>()
        mockConfigServiceExt.whenListApplications().answer(appName)
        mockConfigServiceExt.whenListHosts().answer(listOf(hostName))
        mockConfigServiceExt.whenUpdateProperty { capture(updateSlot) }
            .answer(UpdatePropertyResponse.Type.OK)

        // when
        insertProperty().expectRight()
        insertProperty().expectRight()

        // then
        verify(exactly = 2) { mockConfigService.updateProperty(any(), any()) }
        updateSlot.captured.also { update ->
            update.applicationName shouldBeEqualTo appName
            update.hostName shouldBeEqualTo hostName
            update.propertyName shouldBeEqualTo "someName"
            update.propertyValue shouldBeEqualTo "234"
        }
    }

    @Test
    fun `should read property`() {
        // given
        mockConfigServiceExt.whenReadProperty {
            ReadPropertyRequest.newBuilder()
                .setPropertyName("propertyName")
                .setApplicationName(appName)
                .setHostName(hostName)
                .build()
        }.answer(
            PropertyItem.newBuilder()
                .setPropertyName("propertyName")
                .setPropertyValue("value")
                .setApplicationName(appName)
                .build()
        )

        // then
        dashboardClient.getProperty(appName, hostName, "propertyName")
            .expectRight()
            .shouldNotBeNull()
    }

    @Test
    fun `should return empty JSON if property doesn't exist`() {
        val readPropertyRequest = slot<ReadPropertyRequest>()
        // given
        mockConfigServiceExt.whenReadProperty {
            capture(readPropertyRequest)
        }.answer(null)

        // then
        dashboardClient.getProperty(appName, hostName, "propertyName")
            .expectRight()
            .shouldBeNull()
        readPropertyRequest.captured.also {
            it.propertyName shouldBeEqualTo "propertyName"
            it.hostName shouldBeEqualTo hostName
        }
    }

    @Test
    fun `should throw an exception if a conflict is found`() {
        mockConfigServiceExt.whenListApplications().answer(appName)
        mockConfigServiceExt.whenListHosts().answer(listOf(hostName))
        mockConfigServiceExt.whenUpdateProperty { any() }
            .answer(UpdatePropertyResponse.Type.UPDATE_CONFLICT)
        val err = insertProperty()
            .expectLeft()
        err.errorCode shouldBeEqualTo "update.conflict"
    }

    @Test
    fun `should create a host if it's not found`() {
        // given
        mockConfigServiceExt.whenListApplications().answer(appName)
        mockConfigServiceExt.whenListHosts().answer(emptyList())
        mockConfigServiceExt.whenCreateHost { any() }.answer(
            CreateHostResponse.newBuilder()
                .setType(CreateHostResponse.Type.OK)
                .build()
        )
        mockConfigServiceExt.whenUpdateProperty { any() }
            .answer(UpdatePropertyResponse.Type.OK)
        insertProperty()

        verify(exactly = 1) { mockConfigService.createHost(any(), any()) }
    }

    @Test
    fun `should delete a property`() {
        // given
        val deleteSlot = slot<DeletePropertyRequest>()
        mockConfigServiceExt.whenDeleteProperty { capture(deleteSlot) }
            .answer(
                DeletePropertyResponse.newBuilder()
                    .setType(DeletePropertyResponse.Type.OK)
                    .build()
            )

        // when
        dashboardClient.deleteProperty(appName, hostName, "some property")
            .expectRight()

        // then
        deleteSlot.captured.also { deletePropertyRequest ->
            deletePropertyRequest.applicationName shouldBeEqualTo appName
            deletePropertyRequest.hostName shouldBeEqualTo hostName
            deletePropertyRequest.propertyName shouldBeEqualTo "some property"
            deletePropertyRequest.version shouldBeEqualTo 1
        }
    }

    @Test
    fun testImportProperties() {
        // given
        mockConfigServiceExt.whenListApplications()
            .answer(appName)
        mockConfigServiceExt.whenListHosts()
            .answer(listOf("srvd2", "srvd1"))
        mockConfigServiceExt.whenReadProperty { any() }
            .answer(null)
        mockConfigServiceExt.whenUpdateProperty { any() }
            .answer(UpdatePropertyResponse.Type.OK)

        // when
        val result = dashboardClient.importProperties(
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
        )

        // then
        result.expectRight()
    }

    @Test
    fun testImportIllegalFormat() {
        dashboardClient.importProperties(appName, "illegal input")
            .expectLeft()
            .should { httpCode == 400 }
            .should { errorCode == "illegal.format" }
    }

    private fun insertProperty(): Either<DashboardHttpFailure, Unit> {
        return dashboardClient.updateProperty(
            appName,
            hostName,
            "someName",
            "234",
            "b350bfd5-9f0b-4d3c-b2bf-ec6c429181a8"
        )
    }
}