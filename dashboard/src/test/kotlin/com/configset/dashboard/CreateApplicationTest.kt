package com.configset.dashboard

import com.configset.sdk.proto.ApplicationCreatedResponse
import com.configset.sdk.proto.ApplicationsResponse
import io.grpc.stub.StreamObserver
import io.mockk.every
import org.amshove.kluent.shouldBe
import org.junit.Test

class CreateApplicationTest : BaseDashboardTest() {

    @Test
    fun testNoApplications() {
        every { mockConfigService.listApplications(any(), any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val observer = (it.invocation.args[1] as StreamObserver<ApplicationsResponse>)
            observer.onNext(ApplicationsResponse.newBuilder().build())
            observer.onCompleted()
        }
        val res = executeGetRequest("/application/list", List::class.java)
        res.isEmpty() shouldBe true
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
        executePostRequest("/application/", mapOf(Pair("appName", "testApp")), Any::class.java)
    }
}