package com.letsconfig

import com.letsconfig.network.grpc.common.PropertiesChangesResponse
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue


class GrpcReconnectionTest {

    @JvmField
    @Rule
    val serviceRule = ServiceRule()

    @Test
    fun testReconnection() {
        val subscriberId = "123"

        serviceRule.createApplication(TEST_APP_NAME)
        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, 1, "name", "value")
        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, 2, "name2", "value2")
        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, 3, "name3", "value3")

        val itemsQueue = ConcurrentLinkedQueue<PropertiesChangesResponse>()
        serviceRule.subscribeTestApplication(subscriberId, 2)
        serviceRule.watchChanges(subscriberId, itemsQueue)

        Thread.sleep(serviceRule.updateDelayMs * 2)

        Assert.assertEquals(1, itemsQueue.size)
    }
}