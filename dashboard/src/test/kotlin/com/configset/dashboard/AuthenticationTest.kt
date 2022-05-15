package com.configset.dashboard

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AuthenticationTest : BaseDashboardTest() {

    @Test
    fun `should check auth header`() {
        val res = buildGetRequest("/application/list")
        res.removeHeader("Authorization")
        executeRequest<Any>(res.build(), OBJECT_MAPPER.constructType(Any::class.java))
            .expectLeft()
            .httpCode shouldBeEqualTo 403
    }

    @Test
    fun `should exclude config API call`() {
        val res = buildGetRequest("/config")
        res.removeHeader("Authorization")
        getConfig()
            .expectRight()
    }

    @Test
    fun `should throw 403 error`() {
        mockConfigServiceExt.whenListApplications()
            .throws(StatusRuntimeException(Status.UNAUTHENTICATED))
        listApplications()
            .expectLeft()
            .httpCode shouldBeEqualTo 403
    }
}