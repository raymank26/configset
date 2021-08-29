package com.configset.server

import com.configset.sdk.proto.PropertyItem
import com.configset.test.fixtures.CrudServiceRule
import com.configset.test.fixtures.TEST_APP_NAME
import com.configset.test.fixtures.TEST_DEFAULT_APP_NAME
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

class GrpcWatchTest {

    @JvmField
    @Rule
    val serviceRule = CrudServiceRule()

    @JvmField
    @Rule
    var globalTimeout: Timeout = Timeout.seconds(10)
    
    @Test
    fun testReceiveInitialProperties() {
        serviceRule.createApplication(TEST_APP_NAME)
        serviceRule.updateProperty(TEST_APP_NAME, "host-$TEST_DEFAULT_APP_NAME", 1, "name", "value")
        serviceRule.subscribeTestApplication(lastKnownVersion = null)

        val receivedItems = serviceRule.watchForChanges(1, 5000).first().itemsList
        receivedItems.size shouldBeEqualTo 1
    }

    @Test
    fun testWatch() {
        serviceRule.subscribeTestApplication(lastKnownVersion = null)

        serviceRule.createApplication(TEST_APP_NAME)
        serviceRule.createHost("srvd2")

        serviceRule.updateProperty(TEST_APP_NAME, "srvd1", 1, "name", "value")
        serviceRule.updateProperty(TEST_APP_NAME, "host-$TEST_DEFAULT_APP_NAME", 2, "name2", "value2")
        serviceRule.updateProperty(TEST_APP_NAME, "srvd2", 3, "name3", "value3") // should be skipped

        val receivedItems = serviceRule.watchForChanges(1, 5000).first().itemsList
        receivedItems.size shouldBeEqualTo 2

        Thread.sleep(serviceRule.updateDelayMs * 2)

        Assert.assertEquals(listOf(PropertyItem.newBuilder().setApplicationName(TEST_APP_NAME).setPropertyName("name")
                .setPropertyValue("value").setVersion(1).build(),
                PropertyItem.newBuilder().setApplicationName(TEST_APP_NAME).setPropertyName("name2").setPropertyValue("value2").setVersion(2).build()
        ), receivedItems)

        serviceRule.deleteProperty(TEST_APP_NAME, "srvd1", "name", 1)

        serviceRule.watchForChanges(1, 5000)
    }
}