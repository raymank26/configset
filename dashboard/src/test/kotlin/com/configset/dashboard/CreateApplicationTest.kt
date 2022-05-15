package com.configset.dashboard

import com.configset.sdk.proto.ApplicationCreatedResponse
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class CreateApplicationTest : BaseDashboardTest() {

    @Test
    fun testNoApplications() {
        mockConfigServiceExt.whenListApplications().answer {
            "sample app"
        }
        val response = listApplications()
            .expectRight()
        response.size shouldBeEqualTo 1
        response[0] shouldBeEqualTo ("sample app")
    }

    @Test
    fun createApplication() {
        mockConfigServiceExt.whenCreateApplication().answer {
            ApplicationCreatedResponse.Type.OK
        }
        createApplication("testApp")
            .expectRight()
    }
}