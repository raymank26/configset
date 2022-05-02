package com.configset.dashboard

import com.configset.sdk.proto.ApplicationsResponse
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.sdk.proto.DeletePropertyRequest
import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.EmptyRequest
import com.configset.sdk.proto.ListHostsResponse
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.ReadPropertyRequest
import com.configset.sdk.proto.ReadPropertyResponse
import com.configset.sdk.proto.SearchPropertiesRequest
import com.configset.sdk.proto.SearchPropertiesResponse
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import io.grpc.stub.StreamObserver
import io.mockk.every
import org.amshove.kluent.shouldNotBeNull

typealias AppName = String
typealias HostName = String

class ServerMockExtension(private val mockConfigService: ConfigurationServiceGrpc.ConfigurationServiceImplBase) {

    fun whenListApplications(): ServerMockContext<EmptyRequest, AppName> {
        return object : ServerMockContext<EmptyRequest, AppName>() {
            override fun answer(supplier: () -> AppName) {
                every { mockConfigService.listApplications(request ?: any(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (invocation.args[1] as StreamObserver<ApplicationsResponse>)
                    observer.onNext(ApplicationsResponse.newBuilder()
                        .addApplications(supplier.invoke())
                        .build())
                    observer.onCompleted()
                }
            }

            override fun throws(ex: Exception) {
                every { mockConfigService.listApplications(request ?: any(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (invocation.args[1] as StreamObserver<ApplicationsResponse>)
                    observer.onError(ex)
                }
            }
        }
    }

    fun whenListHosts(): ServerMockContext<EmptyRequest, List<HostName>> {
        return object : ServerMockContext<EmptyRequest, List<HostName>>() {
            override fun answer(supplier: () -> List<HostName>) {
                every { mockConfigService.listHosts(request ?: any(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    val observer = (invocation.args[1] as StreamObserver<ListHostsResponse>)
                    val builder = ListHostsResponse.newBuilder()
                    supplier.invoke().forEach { builder.addHostNames(it) }
                    observer.onNext(builder.build())
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenUpdateProperty(): ServerMockContext<UpdatePropertyRequest, UpdatePropertyResponse.Type> {
        return object : ServerMockContext<UpdatePropertyRequest, UpdatePropertyResponse.Type>() {
            override fun answer(supplier: () -> UpdatePropertyResponse.Type) {
                every { mockConfigService.updateProperty(request ?: any(), any()) } answers {
                    val request = (it.invocation.args[0] as UpdatePropertyRequest)
                    (request.requestId).shouldNotBeNull()
                    interceptor.invoke(request)
                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<UpdatePropertyResponse>)

                    observer.onNext(UpdatePropertyResponse.newBuilder()
                        .setType(supplier.invoke())
                        .build())
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenReadProperty(): ServerMockContext<ReadPropertyRequest, PropertyItem?> {
        return object : ServerMockContext<ReadPropertyRequest, PropertyItem?>() {
            override fun answer(supplier: () -> PropertyItem?) {
                every { mockConfigService.readProperty(request ?: any(), any()) } answers {
                    val request = (it.invocation.args[0] as ReadPropertyRequest)
                    interceptor.invoke(request)

                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<ReadPropertyResponse>)

                    val propertyItem = supplier.invoke()
                    if (propertyItem != null) {
                        observer.onNext(ReadPropertyResponse.newBuilder()
                            .setHasItem(true)
                            .setItem(propertyItem)
                            .build())
                    } else {
                        observer.onNext(ReadPropertyResponse.newBuilder()
                            .setHasItem(false)
                            .build())
                    }
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenDeleteProperty(): ServerMockContext<DeletePropertyRequest, DeletePropertyResponse> {
        return object : ServerMockContext<DeletePropertyRequest, DeletePropertyResponse>() {
            override fun answer(supplier: () -> DeletePropertyResponse) {
                every { mockConfigService.deleteProperty(request ?: any(), any()) } answers {
                    val request = (it.invocation.args[0] as DeletePropertyRequest)
                    interceptor.invoke(request)
                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<DeletePropertyResponse>)


                    observer.onNext(DeletePropertyResponse.newBuilder()
                        .setType(DeletePropertyResponse.Type.OK)
                        .build())
                    observer.onCompleted()
                }
            }
        }
    }

    fun whenSearchProperties(): ServerMockContext<SearchPropertiesRequest, List<com.configset.sdk.proto.ShowPropertyItem>> {
        return object : ServerMockContext<SearchPropertiesRequest, List<com.configset.sdk.proto.ShowPropertyItem>>() {
            override fun answer(supplier: () -> List<com.configset.sdk.proto.ShowPropertyItem>) {
                every { mockConfigService.searchProperties(request ?: any(), any()) } answers {
                    val request = (it.invocation.args[0] as SearchPropertiesRequest)
                    interceptor.invoke(request)
                    @Suppress("UNCHECKED_CAST")
                    val observer = (it.invocation.args[1] as StreamObserver<SearchPropertiesResponse>)

                    val builder = SearchPropertiesResponse.newBuilder()
                    supplier.invoke().forEach { item -> builder.addItems(item) }

                    observer.onNext(builder
                        .build())
                    observer.onCompleted()
                }
            }
        }
    }
}

abstract class ServerMockContext<R, T> {

    protected var interceptor: (R) -> Unit = {}
    protected var request: R? = null

    fun withRequest(request: R) {
        this.request = request
    }

    fun intercept(validator: (R) -> Unit): ServerMockContext<R, T> {
        this.interceptor = validator
        return this
    }

    abstract fun answer(supplier: () -> T)

    open fun throws(ex: Exception) {
        throw NotImplementedError()
    }
}
