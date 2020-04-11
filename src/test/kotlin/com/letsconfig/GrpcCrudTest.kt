package com.letsconfig

import com.letsconfig.network.grpc.common.ApplicationCreateRequest
import com.letsconfig.network.grpc.common.ApplicationCreatedResponse
import com.letsconfig.network.grpc.common.DeletePropertyRequest
import com.letsconfig.network.grpc.common.DeletePropertyResponse
import com.letsconfig.network.grpc.common.EmptyRequest
import com.letsconfig.network.grpc.common.UpdatePropertyRequest
import com.letsconfig.network.grpc.common.UpdatePropertyResponse
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

        val result: List<String> = serviceRule.blockingClient.listApplications(EmptyRequest.getDefaultInstance()).applicationList
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