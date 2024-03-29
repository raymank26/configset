package com.configset.common.client.grpc

import com.configset.client.proto.ConfigurationServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils

const val AUTH_KEY = "Authentication"

class ConfigSetClient(private val channel: ManagedChannel) {

    constructor(hostname: String, port: Int) : this(
        ManagedChannelBuilder.forAddress(hostname, port)
            .usePlaintext()
            .build()
    )

    val blockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub =
        ConfigurationServiceGrpc.newBlockingStub(channel)
            .withInterceptors(DeadlineInterceptor(5000))
    val asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub = ConfigurationServiceGrpc.newStub(channel)

    fun getAuthBlockingClient(accessToken: String): ConfigurationServiceGrpc.ConfigurationServiceBlockingStub {
        val meta = Metadata()
        meta.put(Metadata.Key.of(AUTH_KEY, Metadata.ASCII_STRING_MARSHALLER), accessToken)
        return MetadataUtils.attachHeaders(blockingClient, meta)
    }

    fun stop() {
        channel.shutdown()
    }
}
