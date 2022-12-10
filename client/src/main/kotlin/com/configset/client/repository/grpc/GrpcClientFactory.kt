package com.configset.client.repository.grpc

import com.configset.client.proto.ConfigurationServiceGrpc

interface GrpcClientFactory {
    fun createAsyncClient(): ConfigurationServiceGrpc.ConfigurationServiceStub
}
