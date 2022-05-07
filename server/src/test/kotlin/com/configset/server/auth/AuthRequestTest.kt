package com.configset.server.auth

import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.test.fixtures.CrudServiceRule
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.with
import org.junit.Rule
import org.junit.Test

class AuthRequestTest {

    @JvmField
    @Rule
    val serviceRule = CrudServiceRule()

    @Test
    fun testNotAuthenticated() {
        invoking {
            serviceRule.nonAuthBlockingClient.createApplication(ApplicationCreateRequest.newBuilder()
                .setRequestId("request-id")
                .setApplicationName("app-name")
                .build())
        }
            .shouldThrow(StatusRuntimeException::class)
            .with { status.code == Status.Code.UNAUTHENTICATED }
            .shouldBeTrue()
    }
}