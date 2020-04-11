package com.letsconfig.network.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors

class GrpcConfigurationServer(grpcConfigurationService: GrpcConfigurationService) {

    private val server: Server = ServerBuilder.forPort(8080)
            .addService(ServerInterceptors.intercept(grpcConfigurationService, LoggingInterceptor()))
            .build()


    fun start() {
        server.start()
    }

    fun stop() {
        server.shutdown()
    }
}