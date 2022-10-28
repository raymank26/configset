package com.configset.server.functional.auth

import com.configset.client.proto.ApplicationCreateRequest
import com.configset.server.functional.CrudServiceRule
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.with
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AuthRequestTest {

    companion object {
        @JvmStatic
        @RegisterExtension
        val serviceRule = CrudServiceRule()
    }

    @Test
    fun testNotAuthenticated() {
        invoking {
            serviceRule.nonAuthBlockingClient.createApplication(
                ApplicationCreateRequest.newBuilder()
                    .setRequestId("request-id")
                    .setApplicationName("app-name")
                    .build()
            )
        }
            .shouldThrow(StatusRuntimeException::class)
            .with { status.code == Status.Code.UNAUTHENTICATED }
            .shouldBeTrue()
    }
}
