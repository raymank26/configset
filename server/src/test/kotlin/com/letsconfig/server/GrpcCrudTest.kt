package com.letsconfig.server

import com.letsconfig.sdk.proto.ApplicationCreateRequest
import com.letsconfig.sdk.proto.ApplicationCreatedResponse
import com.letsconfig.sdk.proto.CreateHostRequest
import com.letsconfig.sdk.proto.DeletePropertyRequest
import com.letsconfig.sdk.proto.DeletePropertyResponse
import com.letsconfig.sdk.proto.EmptyRequest
import com.letsconfig.sdk.proto.SearchPropertiesRequest
import com.letsconfig.sdk.proto.UpdatePropertyRequest
import com.letsconfig.sdk.proto.UpdatePropertyResponse
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class GrpcCrudTest {

    @JvmField
    @Rule
    val serviceRule = ServiceRule()

    @Test
    fun testCreateHost() {
        val expectedHostName = "someHost"
        serviceRule.blockingClient.createHost(CreateHostRequest.newBuilder().setRequestId(serviceRule.createRequestId()).setHostName(expectedHostName).build())
        val response = serviceRule.blockingClient.listHosts(EmptyRequest.getDefaultInstance()).hostNamesList.map { it }
        response.contains(expectedHostName) shouldBe true
    }

    @Test
    fun testCreateApplication() {
        val appName = "Some name"
        val response = serviceRule.blockingClient.createApplication(ApplicationCreateRequest.newBuilder()
                .setRequestId(serviceRule.createRequestId())
                .setApplicationName(appName).build())
        Assert.assertEquals(ApplicationCreatedResponse.Type.OK, response.type)

        val result: List<String> = serviceRule.blockingClient.listApplications(EmptyRequest.getDefaultInstance()).applicationsList
        Assert.assertEquals(listOf(appName), result)
    }

    @Test
    fun testDelete() {
        serviceRule.createApplication(TEST_APP_NAME)
        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, null, "test", "value")
        serviceRule.deleteProperty(TEST_APP_NAME, TEST_HOST, "test", 1)
        val res = serviceRule.blockingClient.searchProperties(SearchPropertiesRequest.newBuilder()
                .setApplicationName(TEST_APP_NAME)
                .setHostName(TEST_HOST)
                .setPropertyName("test")
                .build())
        res.itemsCount shouldBeEqualTo 0
    }

    @Test
    fun testDeleteConflict() {
        serviceRule.createApplication(TEST_APP_NAME)
        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, null, "test", "value")
        serviceRule.deleteProperty(TEST_APP_NAME, TEST_HOST, "test", 1123, DeletePropertyResponse.Type.DELETE_CONFLICT)
        val res = serviceRule.blockingClient.searchProperties(SearchPropertiesRequest.newBuilder()
                .setApplicationName(TEST_APP_NAME)
                .setHostName(TEST_HOST)
                .setPropertyName("test")
                .build())
        res.itemsCount shouldBeEqualTo 1
    }

    @Test
    fun testAddPropertyNoApplication() {
        val result = serviceRule.blockingClient.updateProperty(UpdatePropertyRequest.newBuilder()
                .setRequestId(serviceRule.createRequestId())
                .setApplicationName("Some app")
                .setHostName("Some host")
                .setPropertyName("Prop name")
                .setPropertyValue("Some value")
                .setVersion(123L)
                .build())

        Assert.assertEquals(UpdatePropertyResponse.Type.APPLICATION_NOT_FOUND, result.type)
    }

    @Test
    fun testDeletePropertyNotFoundWithApp() {
        serviceRule
                .createApplication("test-app")
        testDeletePropertyNotFoundWithoutApp()
    }

    @Test
    fun testDeletePropertyNotFoundWithoutApp() {
        val res = serviceRule.blockingClient.deleteProperty(DeletePropertyRequest.newBuilder()
                .setRequestId(serviceRule.createRequestId())
                .setApplicationName("test-app")
                .setPropertyName("Prop")
                .setHostName("host")
                .setVersion(1)
                .build())
        Assert.assertEquals(DeletePropertyResponse.Type.PROPERTY_NOT_FOUND, res.type)
    }
}