package com.configset.server

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Rule
import org.junit.Test

class GrpcPostConnectionTest {

    @JvmField
    @Rule
    val serviceRule = ServiceRule()

    @Test
    fun testSubscribeAfterCreation() {
        serviceRule.createApplication(TEST_APP_NAME)
        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, 1, "name", "value")
        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, 2, "name2", "value2")
        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, 3, "name3", "value3")

        serviceRule.subscribeTestApplication(lastKnownVersion = 2)

        val response = serviceRule.watchForChanges(1, serviceRule.updateDelayMs * 2)
        response.first().itemsCount shouldBeEqualTo 1
    }
}