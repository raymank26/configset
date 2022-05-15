package com.configset.dashboard

import arrow.core.Either
import arrow.core.left
import com.configset.sdk.client.ConfigSetClient
import com.configset.sdk.extension.createLoggerStatic
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.test.fixtures.ACCESS_TOKEN
import com.configset.test.fixtures.SERVER_PORT
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule
import io.mockk.spyk
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.amshove.kluent.shouldNotBeNull
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.util.*

private val LOG = createLoggerStatic<BaseDashboardTest>()
val OBJECT_MAPPER = ObjectMapper()

abstract class BaseDashboardTest {

    private lateinit var okHttp: OkHttpClient
    private lateinit var koinApp: KoinApplication

    lateinit var mockConfigService: ConfigurationServiceGrpc.ConfigurationServiceImplBase
    lateinit var mockConfigServiceExt: ServerMockExtension

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

        okHttp = OkHttpClient()
        setUp()
    }

    open fun setUp() {
    }

    fun buildGetRequest(
        endpoint: String,
        queryParams: Map<String, String> = emptyMap(),
    ): Request.Builder {

        val urlBuilder = HttpUrl.Builder()
            .scheme("http")
            .host("localhost")
            .port(9299)
            .addPathSegments("api$endpoint")
        for ((key, value) in queryParams) {
            urlBuilder.addQueryParameter(key, value)
        }
        return Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer $ACCESS_TOKEN")
    }

    fun <T> executeRequest(request: Request, responseClass: JavaType): Either<DashboardHttpFailure, T?> {
        val response = okHttp.newCall(request)
            .execute()
        if (response.code / 100 != 2) {
            val errorDetails = response.body?.byteStream().use {
                OBJECT_MAPPER.readTree(it)
            }
            return DashboardHttpFailure(
                response.code,
                errorDetails["code"]?.textValue(),
            ).left()
        }
        val res: T? = response.body?.let {
            it.charStream().use {
                val content = it.readText()
                LOG.debug("JSON received = {}", content)
                val node: JsonNode = OBJECT_MAPPER.readValue(content, JsonNode::class.java)
                if (node is ObjectNode && node.size() == 0) null else OBJECT_MAPPER.convertValue<T>(node, responseClass)
            }
        }
        return Either.Right(res)
    }

    private fun <T> executeGetRequest(
        endpoint: String,
        responseClass: JavaType,
        queryParams: Map<String, String> = emptyMap(),
    ): Either<DashboardHttpFailure, T?> {
        val urlBuilder = buildGetRequest(endpoint, queryParams)
        return executeRequest(urlBuilder.build(), responseClass)
    }

    private fun <T> executePostRequest(
        endpoint: String,
        bodyParams: Map<String, String>,
        responseClass: JavaType,
        requestId: String = UUID.randomUUID().toString(),
    ): Either<DashboardHttpFailure, T?> {

        val formBody = FormBody.Builder()
        bodyParams.forEach { (key, value) -> formBody.add(key, value) }
        formBody.add("requestId", requestId)
        val response = okHttp.newCall(Request.Builder().url("http://localhost:9299/api$endpoint")
            .header("Authorization", "Bearer $ACCESS_TOKEN")
            .post(formBody.build())
            .build()).execute()
        if (response.code / 100 != 2) {
            val errorDetails = response.body?.byteStream().use {
                OBJECT_MAPPER.readTree(it)
            }
            return DashboardHttpFailure(
                response.code,
                errorDetails["code"]?.textValue(),
            ).left()
        }
        if (response.body?.contentLength() == 0L) {
            return Either.Right(null)
        }
        return Either.Right(response.body?.byteStream().use {
            OBJECT_MAPPER.readValue(it, responseClass)
        })
    }

    fun getProperty(appName: String, hostName: String): Either<DashboardHttpFailure, ShowPropertyItem?> {
        return executeGetRequest(
            "/property/get",
            OBJECT_MAPPER.typeFactory.constructType(ShowPropertyItem::class.java),
            mapOf(
                Pair("applicationName", appName),
                Pair("hostName", hostName),
                Pair("propertyName", "propertyName")
            ))
    }

    fun updateProperty(
        applicationName: String,
        hostName: String,
        propertyName: String,
        propertyValue: String,
        requestId: String,
    ): Either<DashboardHttpFailure, Unit> {
        return executePostRequest<Unit>("/property/update", mapOf(
            Pair("applicationName", applicationName),
            Pair("hostName", hostName),
            Pair("propertyName", propertyName),
            Pair("propertyValue", propertyValue)
        ), OBJECT_MAPPER.typeFactory.constructType(Unit::class.java), requestId)
            .map { }
    }

    fun searchProperties(
        applicationName: String? = null, hostName: String? = null, propertyName: String? = null,
        propertyValue: String? = null,
    ): Either<DashboardHttpFailure, List<ShowPropertyItem>> {
        val queryParams = mutableMapOf<String, String>()
        if (applicationName != null) {
            queryParams["applicationName"] = applicationName
        }
        if (hostName != null) {
            queryParams["hostName"] = hostName
        }
        if (propertyName != null) {
            queryParams["propertyName"] = propertyName
        }
        if (propertyValue != null) {
            queryParams["propertyValue"] = propertyValue
        }
        val responseClass = OBJECT_MAPPER
            .typeFactory
            .constructCollectionType(ArrayList::class.java, ShowPropertyItem::class.java)
        return executeGetRequest<List<ShowPropertyItem>>("/property/search", responseClass, queryParams)
            .map { it!! }
    }

    fun importProperties(appName: String, content: String): Either<DashboardHttpFailure, Unit> {
        return executePostRequest<Unit>("/property/import",
            mapOf(
                Pair("applicationName", appName),
                Pair("properties", content)),
            OBJECT_MAPPER.typeFactory.constructType(Any::class.java)
        ).map { }
    }

    fun deleteProperty(appName: String, hostName: String, propertyName: String): Either<DashboardHttpFailure, Unit> {
        return executePostRequest<Unit>("/property/delete", mapOf(
            Pair("applicationName", appName),
            Pair("hostName", hostName),
            Pair("propertyName", propertyName),
            Pair("version", "1")
        ), OBJECT_MAPPER.constructType(Unit::class.java), requestId = "1239")
            .map { }
    }

    fun listApplications(): Either<DashboardHttpFailure, List<String>> {
        return executeGetRequest<List<String>>("/application/list",
            OBJECT_MAPPER.typeFactory.constructCollectionType(ArrayList::class.java, String::class.java))
            .map { it!! }
    }

    fun createApplication(appName: String): Either<DashboardHttpFailure, Unit> {
        return executePostRequest<Unit>("/application/", mapOf(Pair("appName", appName)),
            OBJECT_MAPPER.constructType(Unit::class.java))
            .map { }
    }

    fun getConfig(): Either<DashboardHttpFailure, Map<String, String>> {
        val res = buildGetRequest("/config")
        res.removeHeader("Authorization")
        return executeRequest<Map<String, String>>(res.build(),
            OBJECT_MAPPER.typeFactory.constructMapType(HashMap::class.java, String::class.java, String::class.java))
            .map { it!! }
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

fun <L, R> Either<L, R>.expectLeft(): L {
    when (this) {
        is Either.Left -> return value
        is Either.Right -> {
            fail("Expected Left, but Right is given $value")
            error("Unreachable")
        }
    }
}

fun <L, R> Either<L, R>.expectRight(): R {
    when (this) {
        is Either.Right -> return value
        is Either.Left -> {
            fail("Expected Right, but Left is given $value")
            error("Unreachable")
        }
    }
}

