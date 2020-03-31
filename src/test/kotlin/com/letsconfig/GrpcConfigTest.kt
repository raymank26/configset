package com.letsconfig

import com.letsconfig.network.grpc.common.PropertyItem
import com.letsconfig.network.grpc.common.SubscribeApplicationRequest
import com.letsconfig.network.grpc.common.SubscriberInfoRequest
import io.grpc.stub.StreamObserver
import org.awaitility.Awaitility
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.ConcurrentLinkedQueue

class GrpcConfigTest {

    @JvmField
    @Rule

    val serviceRule = ServiceRule()

    @JvmField
    @Rule
    var globalTimeout: Timeout = Timeout.seconds(10)

    val log = log()

    @Test
    fun testCreateApplication() {
        val subscriberId = "123"
        val itemsQueue = ConcurrentLinkedQueue<PropertyItem>()

        serviceRule.blockingClient.subscribeApplication(SubscribeApplicationRequest.newBuilder()
                .setApplicationName("test-app")
                .setDefaultApplicationName("my-app")
                .setHostName("srvd1")
                .setSubscriberId(subscriberId)
                .build())
        val value = object : StreamObserver<PropertyItem> {
            override fun onNext(value: PropertyItem?) {
                itemsQueue.add(value)
            }

            override fun onError(t: Throwable?) {
                log.error("onError called", t)
                Assert.fail()
            }

            override fun onCompleted() {
            }
        }
        serviceRule.asyncClient.watchChanges(SubscriberInfoRequest.newBuilder().setId(subscriberId).build(), value)

        serviceRule.createApplication("test-app")

        serviceRule.updateProperty("test-app", "srvd1", 1, "name", "value")
        serviceRule.updateProperty("test-app", "srvd1", 2, "name2", "value2")

        Awaitility.await().until {
            itemsQueue.size == 2
        }
        Thread.sleep(2000)

        Assert.assertEquals(setOf(PropertyItem.newBuilder().setApplicationName("test-app").setPropertyName("name")
                .setPropertyValue("value").setVersion(1).build(),
                PropertyItem.newBuilder().setApplicationName("test-app").setPropertyName("name2").setPropertyValue("value2").setVersion(2).build()
        ), itemsQueue.toSet())
    }
}