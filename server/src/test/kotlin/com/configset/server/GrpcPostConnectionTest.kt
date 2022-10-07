package com.configset.server

import com.configset.test.fixtures.CrudServiceRule
import com.configset.test.fixtures.TEST_APP_NAME
import com.configset.test.fixtures.TEST_HOST
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class GrpcPostConnectionTest {

    @Test
    fun testSubscribeAfterCreation() {
        serviceRule.createApplication(TEST_APP_NAME)
        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, 1, "name", "value")
        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, 2, "name2", "value2")

        serviceRule.subscribeTestApplication(lastKnownVersion = 2)

        serviceRule.updateProperty(TEST_APP_NAME, TEST_HOST, 3, "name3", "value3")

        val response = serviceRule.watchForChanges(1, serviceRule.updateDelayMs * 10)
        response.first().itemsCount shouldBeEqualTo 1
    }

    companion object {
        @JvmStatic
        @RegisterExtension
        val serviceRule = CrudServiceRule()
    }
}