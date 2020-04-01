package com.letsconfig

import com.letsconfig.network.grpc.common.ApplicationSnapshotResponse
import com.letsconfig.network.grpc.common.PropertyItem
import com.letsconfig.network.grpc.common.SubscribeApplicationRequest
import com.letsconfig.network.grpc.common.SubscriberInfoRequest
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

    val log = createLogger()

    @Test
    fun testWatch() {
        val subscriberId = "123"
        val itemsQueue = ConcurrentLinkedQueue<PropertyItem>()

        val subscribeResponse: ApplicationSnapshotResponse = serviceRule.blockingClient.subscribeApplication(SubscribeApplicationRequest.newBuilder()
                .setApplicationName("test-app")
                .setDefaultApplicationName("my-app")
                .setHostName("srvd1")
                .setSubscriberId(subscriberId)
                .build())
        Assert.assertEquals(0, subscribeResponse.itemsList.size)
        serviceRule.asyncClient.watchChanges(SubscriberInfoRequest.newBuilder().setId(subscriberId).build(), QueueStreamObserver(itemsQueue))

        serviceRule.createApplication("test-app")

        serviceRule.updateProperty("test-app", "srvd1", 1, "name", "value")
        serviceRule.updateProperty("test-app", "srvd1", 2, "name2", "value2")

        Awaitility.await().untilAsserted {
            Assert.assertEquals(2, itemsQueue.size)
        }
        Thread.sleep(2000)

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