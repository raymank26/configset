package com.letsconfig

import com.letsconfig.network.grpc.common.PropertyItem
import com.letsconfig.network.grpc.common.SubscribeApplicationRequest
import com.letsconfig.network.grpc.common.SubscriberInfoRequest
import io.grpc.stub.StreamObserver
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class GrpcConfigTest {

    @JvmField
    @Rule
    val serviceRule = ServiceRule()

    val log = log()

    @Test
    fun testCreateApplication() {
        val subscriberId = "123"
        serviceRule.blockingClient.subscribeApplication(SubscribeApplicationRequest.newBuilder()
                .setApplicationName("test-app")
                .setDefaultApplicationName("my-app")
                .setHostName("srvd1")
                .setSubscriberId(subscriberId)
                .build())

        val value = object : StreamObserver<PropertyItem> {
            override fun onNext(value: PropertyItem?) {
                println("HERE")
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
        Thread.sleep(5000)
    }
}