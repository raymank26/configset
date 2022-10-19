package com.configset.client

import com.configset.client.repository.grpc.GrpcClientFactory
import com.configset.client.repository.grpc.GrpcConfigurationRepository
import com.configset.sdk.proto.ConfigurationServiceGrpc
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseClientTest {

    lateinit var clientUtil: ClientTestUtil

    val defaultConfiguration: Configuration by lazy {
        registry.getConfiguration(APP_NAME)
    }

    private lateinit var registry: ConfigurationRegistry

    @BeforeEach
    fun before() {
        clientUtil = ClientTestUtil()
        clientUtil.start()
        val repository = GrpcConfigurationRepository(
            applicationHostname = APP_NAME,
            defaultApplicationName = APP_NAME,
            grpcClientFactory = object : GrpcClientFactory {
                override fun createAsyncClient(): ConfigurationServiceGrpc.ConfigurationServiceStub {
                    return clientUtil.asyncClient
                }
            },
            reconnectionTimeoutMs = 1000
        )
        registry = ConfigurationRegistryFactory.getConfiguration(repository)
        setUp()
    }

    open fun setUp() {
    }

    @AfterEach
    fun after() {
        clientUtil.stop()
        registry.stop()
    }
}

const val APP_NAME = "test"