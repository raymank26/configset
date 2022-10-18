package com.configset.dashboard

import com.configset.sdk.Application
import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.sdk.proto.ApplicationCreatedResponse
import com.configset.sdk.proto.ApplicationDeleteRequest
import com.configset.sdk.proto.ApplicationDeletedResponse
import com.configset.sdk.proto.ApplicationUpdateRequest
import com.configset.sdk.proto.ApplicationUpdatedResponse
import com.configset.sdk.proto.ApplicationsResponse
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.sdk.proto.CreateHostRequest
import com.configset.sdk.proto.CreateHostResponse
import com.configset.sdk.proto.DeletePropertyRequest
import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.ListHostsResponse
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.ReadPropertyRequest
import com.configset.sdk.proto.ReadPropertyResponse
import com.configset.sdk.proto.SearchPropertiesRequest
import com.configset.sdk.proto.SearchPropertiesResponse
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import io.grpc.stub.StreamObserver
import io.mockk.MockKMatcherScope
import io.mockk.every

typealias HostName = String

class ServerMockExtension(private val mockConfigService: ConfigurationServiceGrpc.ConfigurationServiceImplBase) {

    fun whenListApplications(): ServerMockContext<List<Application>> {
        return object : ServerMockContext<List<Application>>() {
            override fun answer(response: List<Application>) {
                every { mockConfigService.listApplications(any(), any()) } answers {

                    @Suppress("UNCHECKED_CAST")
                    val observer = (invocation.args[1] as StreamObserver<ApplicationsResponse>)
                    observer.onNext(run {
                        val builder = ApplicationsResponse.newBuilder()
                        response.forEach {
                            builder.addApplications(
                                com.configset.sdk.proto.Application.newBuilder()
                                    .setId(it.id.id.toString())
                                    .setApplicationName(it.name)
                                    .build()
                            )
                        }
                        builder.build()
                    })
                    observer.onCompleted()
                }
            }

            override fun throws(ex: Exception) {
                every { mockConfigService.listApplications(any(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (invocation.args[1] as StreamObserver<ApplicationsResponse>)
                    observer.onError(ex)
                }
            }
        }
    }

    fun whenCreateApplication(request: MockKMatcherScope.() -> ApplicationCreateRequest):
            ServerMockContext<ApplicationCreatedResponse.Type> {

        return object : ServerMockContext<ApplicationCreatedResponse.Type>() {
            override fun answer(response: ApplicationCreatedResponse.Type) {
                every { mockConfigService.createApplication(request(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<ApplicationCreatedResponse>)
                    observer.onNext(
                        ApplicationCreatedResponse.newBuilder()
                            .setType(response)
                            .build()
                    )
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenUpdateApplication(request: MockKMatcherScope.() -> ApplicationUpdateRequest):
            ServerMockContext<ApplicationUpdatedResponse.Type> {

        return object : ServerMockContext<ApplicationUpdatedResponse.Type>() {
            override fun answer(response: ApplicationUpdatedResponse.Type) {
                every { mockConfigService.updateApplication(request(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<ApplicationUpdatedResponse>)
                    observer.onNext(
                        ApplicationUpdatedResponse.newBuilder()
                            .setType(response)
                            .build()
                    )
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenDeleteApplication(request: MockKMatcherScope.() -> ApplicationDeleteRequest):
            ServerMockContext<ApplicationDeletedResponse> {

        return object : ServerMockContext<ApplicationDeletedResponse>() {
            override fun answer(response: ApplicationDeletedResponse) {
                every { mockConfigService.deleteApplication(request(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<ApplicationDeletedResponse>)

                    observer.onNext(response)
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenListHosts(): ServerMockContext<List<HostName>> {
        return object : ServerMockContext<List<HostName>>() {
            override fun answer(response: List<HostName>) {
                every { mockConfigService.listHosts(any(), any()) } answers {

                    @Suppress("UNCHECKED_CAST")
                    val observer = (invocation.args[1] as StreamObserver<ListHostsResponse>)
                    val builder = ListHostsResponse.newBuilder()
                    response.forEach { builder.addHostNames(it) }
                    observer.onNext(builder.build())
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenCreateHost(request: MockKMatcherScope.() -> CreateHostRequest): ServerMockContext<CreateHostResponse> {
        return object : ServerMockContext<CreateHostResponse>() {
            override fun answer(response: CreateHostResponse) {
                every { mockConfigService.createHost(request(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (invocation.args[1] as StreamObserver<CreateHostResponse>)
                    observer.onNext(response)
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenUpdateProperty(request: MockKMatcherScope.() -> UpdatePropertyRequest): ServerMockContext<UpdatePropertyResponse.Type> {
        return object : ServerMockContext<UpdatePropertyResponse.Type>() {
            override fun answer(response: UpdatePropertyResponse.Type) {
                every { mockConfigService.updateProperty(request(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<UpdatePropertyResponse>)

                    observer.onNext(
                        UpdatePropertyResponse.newBuilder()
                            .setType(response)
                            .build()
                    )
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenReadProperty(request: MockKMatcherScope.() -> ReadPropertyRequest): ServerMockContext<PropertyItem?> {
        return object : ServerMockContext<PropertyItem?>() {
            override fun answer(response: PropertyItem?) {
                every { mockConfigService.readProperty(request(), any()) } answers {

                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<ReadPropertyResponse>)

                    if (response != null) {
                        observer.onNext(
                            ReadPropertyResponse.newBuilder()
                                .setHasItem(true)
                                .setItem(response)
                                .build()
                        )
                    } else {
                        observer.onNext(
                            ReadPropertyResponse.newBuilder()
                                .setHasItem(false)
                                .build()
                        )
                    }
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenDeleteProperty(request: MockKMatcherScope.() -> DeletePropertyRequest):
            ServerMockContext<DeletePropertyResponse> {

        return object : ServerMockContext<DeletePropertyResponse>() {
            override fun answer(response: DeletePropertyResponse) {
                every { mockConfigService.deleteProperty(request(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<DeletePropertyResponse>)

                    observer.onNext(response)
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenSearchProperties(request: MockKMatcherScope.() -> SearchPropertiesRequest): ServerMockContext<List<PropertyItem>> {
        return object : ServerMockContext<List<PropertyItem>>() {
            override fun answer(response: List<PropertyItem>) {
                every { mockConfigService.searchProperties(request(), any()) } answers {

                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<SearchPropertiesResponse>)

                    val builder = SearchPropertiesResponse.newBuilder()

                    response.forEach { item -> builder.addItems(item) }

                    observer.onNext(builder.build())
                    observer.onCompleted()
                }
            }
        }
    }
}

abstract class ServerMockContext<T> {

    abstract fun answer(response: T)

    open fun throws(ex: Exception) {
        throw NotImplementedError()
    }
}
