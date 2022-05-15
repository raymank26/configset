package com.configset.dashboard

import com.configset.dashboard.infra.BaseDashboardTest
import com.configset.dashboard.infra.OBJECT_MAPPER
import com.configset.dashboard.infra.expectLeft
import com.configset.dashboard.infra.expectRight
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AuthenticationTest : BaseDashboardTest() {

    @Test
    fun `should check auth header`() {
        val res = dashboardClient.buildGetRequest("/application/list")
        res.removeHeader("Authorization")
        dashboardClient.executeRequest<Any?>(res.build(), OBJECT_MAPPER.constructType(Any::class.java))
            .expectLeft()
            .httpCode shouldBeEqualTo 403
    }

    @Test
    fun `should exclude config API call`() {
        val res = dashboardClient.buildGetRequest("/config")
        res.removeHeader("Authorization")
        dashboardClient.getConfig()
            .expectRight()
    }

    @Test
    fun `should throw 403 error`() {
        mockConfigServiceExt.whenListApplications()
            .throws(StatusRuntimeException(Status.UNAUTHENTICATED))
        dashboardClient.listApplications()
            .expectLeft()
            .httpCode shouldBeEqualTo 403
    }
}