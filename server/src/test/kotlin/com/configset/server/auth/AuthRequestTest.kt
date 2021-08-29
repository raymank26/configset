package com.configset.server.auth

import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.test.fixtures.CrudServiceRule
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class AuthRequestTest {

    @JvmField
    @Rule
    val serviceRule = CrudServiceRule()

    @Test
    fun testNotAuthenticated() {
        var ex: Exception? = null
        try {
            serviceRule.nonAuthBlockingClient.createApplication(ApplicationCreateRequest.newBuilder()
                .setRequestId("request-id")
                .setApplicationName("app-name")
                .build())
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.UNAUTHENTICATED.code) {
                return
            }
            ex = e
        }
        Assert.fail("Not authenticated exception is received, ex = $ex")
    }
}