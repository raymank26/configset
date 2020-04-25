package com.letsconfig.server

import com.letsconfig.sdk.proto.ApplicationCreateRequest
import com.letsconfig.sdk.proto.ApplicationCreatedResponse
import com.letsconfig.sdk.proto.DeletePropertyRequest
import com.letsconfig.sdk.proto.DeletePropertyResponse
import com.letsconfig.sdk.proto.EmptyRequest
import com.letsconfig.sdk.proto.UpdatePropertyRequest
import com.letsconfig.sdk.proto.UpdatePropertyResponse
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class GrpcCrudTest {

    @JvmField
    @Rule
    val serviceRule = ServiceRule()

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
                .build())
        Assert.assertEquals(DeletePropertyResponse.Type.PROPERTY_NOT_FOUND, res.type)
    }
}