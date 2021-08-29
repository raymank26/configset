package com.configset.test.fixtures

import com.configset.sdk.client.ConfigSetClient
import com.configset.sdk.extension.createLogger
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.sdk.proto.PropertiesChangesResponse
import com.configset.sdk.proto.WatchRequest
import com.configset.server.AppConfiguration
import com.configset.server.createAppModules
import com.configset.server.network.grpc.GrpcConfigurationServer
import io.grpc.stub.StreamObserver
import org.junit.rules.ExternalResource
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.util.concurrent.LinkedBlockingDeque

class ServiceStarterRule : ExternalResource() {
    lateinit var blockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    lateinit var nonAuthBlockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub

    lateinit var subscribeStream: StreamObserver<WatchRequest>
    lateinit var asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub

    val accessToken = "access-token"

    val changesQueue = LinkedBlockingDeque<PropertiesChangesResponse>()
    private val log = createLogger()

    private lateinit var koinApp: KoinApplication

    public override fun before() {
        val mainModules = createAppModules(AppConfiguration(mapOf(
            "db_type" to "memory",
            "grpc_port" to "8080",

            "authenticator_type" to "stub",
            "auth_stub.admin_access_token" to accessToken
        )))
        koinApp = startKoin {
            modules(mainModules)
        }
        koinApp.koin.get<GrpcConfigurationServer>().start()

        val configSetClient = ConfigSetClient("localhost", 8080)
        blockingClient = configSetClient.getAuthBlockingClient(accessToken)
        nonAuthBlockingClient = configSetClient.blockingClient
        asyncClient = configSetClient.asyncClient

        subscribeStream = asyncClient.watchChanges(object : StreamObserver<PropertiesChangesResponse> {
            override fun onNext(value: PropertiesChangesResponse) {
                changesQueue.add(value)
            }

            override fun onError(t: Throwable) {
                log.warn("Error occurred in response stream", t)
            }

            override fun onCompleted() {
            }
        })
    }

    public override fun after() {
        stopKoin()
    }
}