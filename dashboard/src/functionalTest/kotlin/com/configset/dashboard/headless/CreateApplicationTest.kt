package com.configset.dashboard.headless

import com.configset.dashboard.FunctionalTest
import com.configset.dashboard.expectRight
import com.configset.sdk.proto.ApplicationCreatedResponse
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class CreateApplicationTest : FunctionalTest() {

    @Test
    fun testNoApplications() {
        mockConfigServiceExt.whenListApplications().answer("sample app")
        val response = dashboardClient.listApplications()
            .expectRight()
        response.size shouldBeEqualTo 1
        response[0] shouldBeEqualTo ("sample app")
    }

    @Test
    fun createApplication() {
        mockConfigServiceExt.whenCreateApplication {
            any()
        }.answer(ApplicationCreatedResponse.Type.OK)

        dashboardClient.createApplication("testApp")
            .expectRight()
    }
}