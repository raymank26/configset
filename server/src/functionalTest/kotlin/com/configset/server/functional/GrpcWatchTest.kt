package com.configset.server.functional

import com.configset.sdk.proto.PropertyItem
import com.configset.server.fixtures.TEST_APP_NAME
import com.configset.server.fixtures.TEST_DEFAULT_APP_NAME
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class GrpcWatchTest {

    companion object {
        @JvmStatic
        @RegisterExtension
        val serviceRule = CrudServiceRule()
    }

    @Test
    fun testReceiveInitialProperties() {
        serviceRule.createApplication(TEST_APP_NAME)
        serviceRule.updateProperty(TEST_APP_NAME, "host-$TEST_DEFAULT_APP_NAME", 1, "name", "value")
        serviceRule.subscribeTestApplication(lastKnownVersion = null)

        val receivedItems = serviceRule.watchForChanges(1, 50000).first().itemsList
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

        Assertions.assertEquals(
            listOf(
                PropertyItem.newBuilder()
                    .setApplicationName(TEST_APP_NAME)
                    .setPropertyName("name")
                    .setPropertyValue("value")
                    .setHostName("srvd1")
                    .setVersion(1)
                    .build(),
                PropertyItem.newBuilder()
                    .setApplicationName(TEST_APP_NAME)
                    .setPropertyName("name2")
                    .setPropertyValue("value2")
                    .setHostName("host-my-app")
                    .setVersion(2)
                    .build()
            ), receivedItems
        )

        serviceRule.deleteProperty(TEST_APP_NAME, "srvd1", "name", 1)

        serviceRule.watchForChanges(1, 5000)
    }
}