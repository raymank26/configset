package com.configset.dashboard

import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.sdk.proto.ApplicationCreatedResponse
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
import com.configset.sdk.proto.ShowPropertyItem
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import io.grpc.stub.StreamObserver
import io.mockk.MockKMatcherScope
import io.mockk.every

typealias AppName = String
typealias HostName = String

class ServerMockExtension(private val mockConfigService: ConfigurationServiceGrpc.ConfigurationServiceImplBase) {

    fun whenListApplications(): ServerMockContext<AppName> {
        return object : ServerMockContext<AppName>() {
            override fun answer(response: AppName) {
                every { mockConfigService.listApplications(any(), any()) } answers {

                    @Suppress("UNCHECKED_CAST")
                    val observer = (invocation.args[1] as StreamObserver<ApplicationsResponse>)
                    observer.onNext(
                        ApplicationsResponse.newBuilder()
                            .addApplications(response)
                            .build()
                    )
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

    fun whenCreateApplication(request: MockKMatcherScope.() -> ApplicationCreateRequest): ServerMockContext<ApplicationCreatedResponse.Type> {
        return object : ServerMockContext<ApplicationCreatedResponse.Type>() {
            override fun answer(response: ApplicationCreatedResponse.Type) {
                every { mockConfigService.createApplication(request(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<ApplicationCreatedResponse>)
                    observer.onNext(
                        ApplicationCreatedResponse.newBuilder()
                            .setType(response).build()
                    )
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

    fun whenSearchProperties(request: MockKMatcherScope.() -> SearchPropertiesRequest): ServerMockContext<List<ShowPropertyItem>> {
        return object : ServerMockContext<List<ShowPropertyItem>>() {
            override fun answer(response: List<ShowPropertyItem>) {
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
