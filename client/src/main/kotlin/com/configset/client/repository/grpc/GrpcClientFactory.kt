package com.configset.client.repository.grpc

import com.configset.sdk.proto.ConfigurationServiceGrpc

fun interface GrpcClientFactory {
    fun createAsyncClient(): ConfigurationServiceGrpc.ConfigurationServiceStub
}