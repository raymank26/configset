package com.letsconfig

import com.letsconfig.network.grpc.common.PropertyItem
import org.awaitility.Awaitility
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.ConcurrentLinkedQueue

class GrpcWatchTest {

    @JvmField
    @Rule
    val serviceRule = ServiceRule()

    @JvmField
    @Rule
    var globalTimeout: Timeout = Timeout.seconds(10)

    @Test
    fun testWatch() {
        val subscriberId = "123"
        val itemsQueue = ConcurrentLinkedQueue<PropertyItem>()

        val subscribeResponse = serviceRule.subscribeTestApplication(subscriberId, null)
        Assert.assertEquals(0, subscribeResponse.itemsList.size)
        Assert.assertEquals(0, itemsQueue.size)

        serviceRule.watchChanges(subscriberId, itemsQueue)

        serviceRule.createApplication("test-app")

        serviceRule.updateProperty(TEST_APP_NAME, "srvd1", 1, "name", "value")
        serviceRule.updateProperty(TEST_APP_NAME, "srvd1", 2, "name2", "value2")

        Awaitility.await().untilAsserted {
            Assert.assertEquals(2, itemsQueue.size)
        }
        Thread.sleep(serviceRule.updateDelayMs * 2)

        Assert.assertEquals(setOf(PropertyItem.newBuilder().setApplicationName("test-app").setPropertyName("name")
                .setPropertyValue("value").setVersion(1).build(),
                PropertyItem.newBuilder().setApplicationName("test-app").setPropertyName("name2").setPropertyValue("value2").setVersion(2).build()
        ), itemsQueue.toSet())

        itemsQueue.clear()

        serviceRule.deleteProperty("test-app", "srvd1", "name")

        Awaitility.await().until {
            itemsQueue.size == 1
        }
    }
}