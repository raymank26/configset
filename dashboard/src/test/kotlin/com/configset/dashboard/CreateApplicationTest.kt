package com.configset.dashboard

import com.configset.dashboard.infra.BaseDashboardTest
import com.configset.dashboard.infra.expectRight
import com.configset.sdk.proto.ApplicationCreatedResponse
import org.amshove.kluent.any
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class CreateApplicationTest : BaseDashboardTest() {

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
        mockConfigServiceExt.whenCreateApplication(any()).answer(ApplicationCreatedResponse.Type.OK)
        dashboardClient.createApplication("testApp")
            .expectRight()
    }
}