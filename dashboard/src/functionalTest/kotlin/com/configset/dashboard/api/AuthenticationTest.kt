package com.configset.dashboard.api

import com.configset.dashboard.FunctionalTest
import com.configset.dashboard.OBJECT_MAPPER
import com.configset.dashboard.expectLeft
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class AuthenticationTest : FunctionalTest() {

    @Test
    fun `should check auth header`() {
        val res = dashboardClient.buildGetRequest("/application/list")
        res.removeHeader("Cookie")
        dashboardClient.executeRequest<Any?>(res.build(), OBJECT_MAPPER.constructType(Any::class.java))
            .expectLeft()
            .httpCode shouldBeEqualTo 403
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