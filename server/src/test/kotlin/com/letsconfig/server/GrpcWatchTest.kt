package com.letsconfig.server

import com.letsconfig.sdk.proto.PropertyItem
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

class GrpcWatchTest {

    @JvmField
    @Rule
    val serviceRule = ServiceRule()

    @JvmField
    @Rule
    var globalTimeout: Timeout = Timeout.seconds(10)

    @Test
    fun testWatch() {
        serviceRule.subscribeTestApplication(lastKnownVersion = null)

        serviceRule.createApplication("test-app")

        serviceRule.updateProperty(TEST_APP_NAME, "srvd1", 1, "name", "value")
        serviceRule.updateProperty(TEST_APP_NAME, "srvd1", 2, "name2", "value2")

        val receivedItems = serviceRule.watchForChanges(1, 5000).first().itemsList
        receivedItems.size shouldBeEqualTo 2

        Thread.sleep(serviceRule.updateDelayMs * 2)

        Assert.assertEquals(listOf(PropertyItem.newBuilder().setApplicationName("test-app").setPropertyName("name")
                .setPropertyValue("value").setVersion(1).build(),
                PropertyItem.newBuilder().setApplicationName("test-app").setPropertyName("name2").setPropertyValue("value2").setVersion(2).build()
        ), receivedItems)

        serviceRule.deleteProperty("test-app", "srvd1", "name", 1)

        serviceRule.watchForChanges(1, 5000)
    }
}