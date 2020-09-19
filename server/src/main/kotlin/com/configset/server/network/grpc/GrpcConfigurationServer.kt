package com.configset.server.network.grpc

import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.netty.NettyServerBuilder
import java.util.concurrent.TimeUnit

class GrpcConfigurationServer(grpcConfigurationService: GrpcConfigurationService, port: Int = 8080) {

    // For keepalive see https://stackoverflow.com/questions/57606303/grpc-arg-keepalive-permit-without-calls-for-java-servers
    private val server: Server = NettyServerBuilder.forPort(port)
            .permitKeepAliveWithoutCalls(true)
            .permitKeepAliveTime(4, TimeUnit.SECONDS)
            .addService(ServerInterceptors.intercept(grpcConfigurationService, LoggingInterceptor()))
            .build()


    fun start() {
        server.start()
    }

    fun stop() {
        server.shutdown()
    }
}