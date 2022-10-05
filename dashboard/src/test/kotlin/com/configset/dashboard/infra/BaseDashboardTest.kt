package com.configset.dashboard.infra

import com.configset.dashboard.App
import com.configset.dashboard.Config
import com.configset.dashboard.DependencyFactory
import com.configset.dashboard.Main
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

abstract class BaseDashboardTest {

    lateinit var mockConfigService: ConfigurationServiceGrpc.ConfigurationServiceImplBase
    lateinit var mockConfigServiceExt: ServerMockExtension
    lateinit var dashboardClient: DashboardClient

    @Rule
    @JvmField
    val grpcCleanup = GrpcCleanupRule()

    lateinit var app: App

    @Before
    fun before() {
        mockConfigService = spyk()
        mockConfigServiceExt = ServerMockExtension(mockConfigService)
        grpcCleanup.register(
            InProcessServerBuilder.forName("mytest")
                .directExecutor()
                .addService(ServerInterceptors.intercept(mockConfigService, AuthCheckInterceptor()))
                .build()
                .start()
        )
        val channel = grpcCleanup.register(InProcessChannelBuilder.forName("mytest").directExecutor().build())

        val config = Config(
            mapOf(
                Pair("config_server.hostname", "localhost"),
                Pair("config_server.port", SERVER_PORT.toString()),
                Pair("dashboard.port", 9299.toString()),
                Pair("serve.static", "false"),
                Pair("authenticator_type", ""),
                Pair("auth.auth_uri", "http://localhost:23982/auth"),
                Pair("auth.redirect_uri", "http://localhost:9299/auth/redirect"),
                Pair("auth.request_token_uri", "http://localhost:23982/token"),
                Pair("auth.client_id", "sample_content_id"),
                Pair("auth.secret_key", "sample_secret_key"),
            )
        )
        app = Main.createApp(object : DependencyFactory(config) {
            override fun configSetClient(): ConfigSetClient {
                return ConfigSetClient(channel)
            }
        })
        app.start()
        dashboardClient = DashboardClient()
    }

    @After
    fun after() {
        app.stop()
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

