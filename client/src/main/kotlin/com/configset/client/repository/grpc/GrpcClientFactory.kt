package com.configset.client.repository.grpc

import com.configset.sdk.proto.ConfigurationServiceGrpc

interface GrpcClientFactory {
    fun createAsyncClient(): ConfigurationServiceGrpc.ConfigurationServiceStub
}