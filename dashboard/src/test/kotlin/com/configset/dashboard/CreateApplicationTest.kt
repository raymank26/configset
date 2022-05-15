package com.configset.dashboard

import com.configset.sdk.proto.ApplicationCreatedResponse
import com.configset.sdk.proto.ApplicationsResponse
import io.grpc.stub.StreamObserver
import io.mockk.every
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class CreateApplicationTest : BaseDashboardTest() {

    @Test
    fun testNoApplications() {
        every { mockConfigService.listApplications(any(), any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val observer = (it.invocation.args[1] as StreamObserver<ApplicationsResponse>)
            observer.onNext(ApplicationsResponse.newBuilder().addApplications("sample app").build())
            observer.onCompleted()
        }
        val response = listApplications()
            .expectRight()
        response.size shouldBeEqualTo 1
        response[0] shouldBeEqualTo ("sample app")
    }

    @Test
    fun createApplication() {
        every { mockConfigService.createApplication(any(), any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val observer = (it.invocation.args[1] as StreamObserver<ApplicationCreatedResponse>)
            observer.onNext(ApplicationCreatedResponse.newBuilder()
                .setType(ApplicationCreatedResponse.Type.OK).build())
            observer.onCompleted()
        }
        createApplication("testApp")
            .expectRight()
    }
}