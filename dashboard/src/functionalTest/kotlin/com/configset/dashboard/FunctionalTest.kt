package com.configset.dashboard

import com.configset.sdk.auth.AuthenticationProvider
import com.configset.sdk.auth.StubAuthenticationProvider.Companion.stubAuthenticationProvider
import com.configset.sdk.client.ConfigSetClient
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.server.fixtures.SERVER_PORT
import com.fasterxml.jackson.databind.ObjectMapper
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.mockk.clearMocks
import io.mockk.spyk
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll

const val EMPTY_ROLES_ACCESS_TOKEN = "access.token.empty.roles"
const val FULL_ROLES_ACCESS_TOKEN = "access.token.full.roles"

abstract class FunctionalTest {

    companion object {

        lateinit var mockConfigService: ConfigurationServiceGrpc.ConfigurationServiceImplBase
        lateinit var mockConfigServiceExt: ServerMockExtension
        lateinit var dashboardClient: DashboardClient

        private lateinit var grpcServer: Server
        private lateinit var channel: ManagedChannel
        private lateinit var app: App

        @JvmStatic
        @BeforeAll
        fun before() {
            mockConfigService = spyk()
            mockConfigServiceExt = ServerMockExtension(mockConfigService)
            grpcServer = InProcessServerBuilder
                .forName("mytest")
                .directExecutor()
                .addService(ServerInterceptors.intercept(mockConfigService, AuthCheckInterceptor()))
                .build()
                .start()
            channel = InProcessChannelBuilder
                .forName("mytest")
                .directExecutor()
                .build()

            val config = Config(
                mapOf(
                    Pair("config_server.hostname", "localhost"),
                    Pair("config_server.port", SERVER_PORT.toString()),
                    Pair("dashboard.port", 9299.toString()),
                    Pair("serve.static", "false"),
                    Pair("authenticator_type", ""),
                    Pair("auth.realm_uri", ""),
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

                override fun authenticationProvider(objectMapper: ObjectMapper): AuthenticationProvider {
                    return stubAuthenticationProvider {
                        addUser(EMPTY_ROLES_ACCESS_TOKEN, "test.user", setOf())
                        addUser(FULL_ROLES_ACCESS_TOKEN, "test.user", setOf("applicationCreator"))
                    }
                }
            })
            app.start()
            dashboardClient = DashboardClient()
        }

        @AfterAll
        @JvmStatic
        fun after() {
            app.stop()
            channel.shutdownNow()
            grpcServer.shutdownNow()
        }
    }

    @AfterEach
    fun afterEach() {
        clearMocks(mockConfigService)
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
