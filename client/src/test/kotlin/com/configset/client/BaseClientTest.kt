package com.configset.client

import com.configset.client.repository.grpc.GrpcConfigurationRepository
import io.grpc.testing.GrpcCleanupRule
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class BaseClientTest {

    @Rule
    @JvmField
    val grpcRule = GrpcCleanupRule()

    val clientUtil = ClientTestUtil(grpcRule)

    val defaultConfiguration: Configuration by lazy {
        registry.getConfiguration(APP_NAME)
    }

    private lateinit var registry: ConfigurationRegistry

    @Before
    fun before() {
        val repository = GrpcConfigurationRepository(
            applicationHostname = APP_NAME,
            defaultApplicationName = APP_NAME,
            grpcClientFactory = {
                clientUtil.asyncClient
            },
            reconnectionTimeoutMs = 1000
        )
        registry = ConfigurationRegistryFactory.getConfiguration(repository)
        setUp()
    }

    open fun setUp() {
    }

    @After
    fun after() {
        registry.stop()
    }
}

const val APP_NAME = "test"