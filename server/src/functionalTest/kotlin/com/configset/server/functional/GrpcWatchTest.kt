package com.configset.server.functional

import com.configset.server.fixtures.TEST_APP_NAME
import com.configset.server.fixtures.TEST_DEFAULT_APP_NAME
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainAny
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
        // given
        serviceRule.createApplication(TEST_APP_NAME)
        serviceRule.updateProperty(TEST_APP_NAME, "host-$TEST_DEFAULT_APP_NAME", 1, "name", "value")
        serviceRule.subscribeTestApplication(lastKnownVersion = null)

        // when
        val receivedItems = serviceRule.watchForChanges(1, 5000).first().itemsList

        // then
        receivedItems.size shouldBeEqualTo 1
    }

    @Test
    fun testWatch() {
        // given
        serviceRule.subscribeTestApplication(lastKnownVersion = null)

        serviceRule.createApplication(TEST_APP_NAME)
        serviceRule.createHost("srvd2")

        serviceRule.updateProperty(TEST_APP_NAME, "srvd1", 1, "name", "value")
        serviceRule.updateProperty(TEST_APP_NAME, "host-$TEST_DEFAULT_APP_NAME", 2, "name2", "value2")
        serviceRule.updateProperty(TEST_APP_NAME, "srvd2", 3, "name3", "value3") // should be skipped

        // when
        val receivedItems = serviceRule.watchForChanges(1, 5000).first().itemsList

        // then
        receivedItems.size shouldBeEqualTo 2

        receivedItems shouldContainAny {
            it.applicationName == TEST_APP_NAME
                    && it.propertyName == "name"
                    && it.propertyValue == "value"
                    && it.hostName == "srvd1"
                    && it.version == 1L
        } shouldContainAny {
            it.applicationName == TEST_APP_NAME
                    && it.propertyName == "name2"
                    && it.propertyValue == "value2"
                    && it.hostName == "host-my-app"
                    && it.version == 2L
        }

        serviceRule.deleteProperty(TEST_APP_NAME, "srvd1", "name", 1)
    }
}