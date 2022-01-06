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
import org.koin.dsl.koinApplication
import java.util.concurrent.LinkedBlockingDeque

const val SERVER_PORT = 8080

const val ACCESS_TOKEN = "access-token"

class ServiceStarterRule : ExternalResource() {

    lateinit var blockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    lateinit var nonAuthBlockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub

    lateinit var subscribeStream: StreamObserver<WatchRequest>
    lateinit var asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub

    val changesQueue = LinkedBlockingDeque<PropertiesChangesResponse>()

    private val log = createLogger()

    private lateinit var koinApp: KoinApplication

    private val configSetClient: ConfigSetClient = ConfigSetClient("localhost", SERVER_PORT)

    public override fun before() {
        val mainModules = createAppModules(AppConfiguration(mapOf(
            "db_type" to "memory",
            "grpc_port" to "8080",

            "authenticator_type" to "stub",
            "auth_stub.admin_access_token" to ACCESS_TOKEN
        )))
        koinApp = koinApplication {
            modules(mainModules)
        }
        koinApp.koin.get<GrpcConfigurationServer>().start()

        blockingClient = configSetClient.getAuthBlockingClient(ACCESS_TOKEN)
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
        configSetClient.stop()
        koinApp.close()
    }
}