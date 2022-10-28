package com.configset.server.network.grpc

import com.configset.common.backend.auth.AuthenticationProvider
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.netty.NettyServerBuilder
import java.util.concurrent.TimeUnit

class GrpcConfigurationServer(
    grpcConfigurationService: GrpcConfigurationService,
    authenticator: AuthenticationProvider,
    port: Int,
) {

    // For keepalive see https://stackoverflow.com/questions/57606303/grpc-arg-keepalive-permit-without-calls-for-java-servers
    private val server: Server = NettyServerBuilder.forPort(port)
        .permitKeepAliveWithoutCalls(true)
        .permitKeepAliveTime(4, TimeUnit.SECONDS)
        .addService(
            ServerInterceptors.intercept(
                grpcConfigurationService,
                LoggingInterceptor(),
                AuthInterceptor(authenticator)
            )
        )
        .build()

    fun start() {
        server.start()
    }

    fun stop() {
        server.shutdownNow()
        server.awaitTermination()
    }
}
