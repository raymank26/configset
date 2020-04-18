package com.letsconfig.server.network.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors

class GrpcConfigurationServer(grpcConfigurationService: GrpcConfigurationService, port: Int = 8080) {

    private val server: Server = ServerBuilder.forPort(port)
            .addService(ServerInterceptors.intercept(grpcConfigurationService, LoggingInterceptor()))
            .build()


    fun start() {
        server.start()
    }

    fun stop() {
        server.shutdown()
    }
}