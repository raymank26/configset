package com.configset.dashboard.infra

import com.configset.dashboard.Config
import com.configset.dashboard.JavalinServer
import com.configset.dashboard.mainModule
import com.configset.sdk.client.ConfigSetClient
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.test.fixtures.SERVER_PORT
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule
import io.mockk.spyk
import org.amshove.kluent.shouldNotBeNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

abstract class BaseDashboardTest {

    private lateinit var koinApp: KoinApplication

    lateinit var mockConfigService: ConfigurationServiceGrpc.ConfigurationServiceImplBase
    lateinit var mockConfigServiceExt: ServerMockExtension
    lateinit var dashboardClient: DashboardClient

    @Rule
    @JvmField
    val grpcCleanup = GrpcCleanupRule()

    @Before
    fun before() {
        mockConfigService = spyk()
        mockConfigServiceExt = ServerMockExtension(mockConfigService)
        grpcCleanup.register(InProcessServerBuilder.forName("mytest")
            .directExecutor()
            .addService(ServerInterceptors.intercept(mockConfigService, AuthCheckInterceptor()))
            .build()
            .start())
        val channel = grpcCleanup.register(InProcessChannelBuilder.forName("mytest").directExecutor().build())

        koinApp = koinApplication {
            modules(mainModule, module {
                single {
                    ConfigSetClient(channel)
                }
            })
        }.properties(mapOf(
            Pair("config", Config(
                mapOf(
                    Pair("config_server.hostname", "localhost"),
                    Pair("config_server.port", SERVER_PORT.toString()),
                    Pair("dashboard.port", 9299.toString()),
                    Pair("serve.static", "false"),
                    Pair("authenticator_type", ""),
                    Pair("client.keycloack_url", "localhost"),
                    Pair("client.keycloack_realm", "sample-realm"),
                    Pair("client.keycloack_clientId", "sample-clientId"),
                )
            ))
        ))
        val server = koinApp.koin.get<JavalinServer>()
        server.start()

        dashboardClient = DashboardClient()
        setUp()
    }

    open fun setUp() {
    }

    @After
    fun after() {
        koinApp.close()
    }
}

private class AuthCheckInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {

        val authHeader = headers.get(Metadata.Key.of("Authentication", Metadata.ASCII_STRING_MARSHALLER))
        authHeader.shouldNotBeNull()
        return next.startCall(call, headers)
    }
}

data class DashboardHttpFailure(
    val httpCode: Int,
    val errorCode: String?,
)
