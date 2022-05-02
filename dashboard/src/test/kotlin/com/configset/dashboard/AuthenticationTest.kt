package com.configset.dashboard

import com.fasterxml.jackson.databind.node.ObjectNode
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.junit.Test

class AuthenticationTest : BaseDashboardTest() {

    @Test
    fun `should check auth header`() {
        val res = buildGetRequest("/application/list")
        res.removeHeader("Authorization")
        invoking {
            executeRequest(res.build(), List::class.java)
        } shouldThrow (AssertionError::class) withMessage ("Expected <200>, actual <403>.")
    }

    @Test
    fun `should exclude config API call`() {
        val res = buildGetRequest("/config")
        res.removeHeader("Authorization")
        executeRequest(res.build(), ObjectNode::class.java)
    }

    @Test
    fun `should throw 403 error`() {
        mockConfigServiceExt.whenListApplications()
            .throws(StatusRuntimeException(Status.UNAUTHENTICATED))
        invoking {
            executeGetRequest("/application/list", List::class.java)
        } shouldThrow (AssertionError::class) withMessage ("Expected <200>, actual <403>.")
    }
}