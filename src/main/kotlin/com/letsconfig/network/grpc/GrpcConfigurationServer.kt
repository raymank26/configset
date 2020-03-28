package com.letsconfig.network.grpc

import io.grpc.Server
import io.grpc.ServerBuilder

class GrpcConfigurationServer(grpcConfigurationService: GrpcConfigurationService) {

    private val server: Server = ServerBuilder.forPort(8080)
            .addService(grpcConfigurationService)
            .build()

    fun start() {
        server.start()
    }

    fun stop() {
        server.shutdown()
    }
}