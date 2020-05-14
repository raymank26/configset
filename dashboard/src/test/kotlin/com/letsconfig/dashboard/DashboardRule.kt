package com.letsconfig.dashboard

import com.fasterxml.jackson.databind.ObjectMapper
import com.letsconfig.sdk.extension.createLoggerStatic
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Assert
import org.junit.rules.ExternalResource
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.util.*

private val LOG = createLoggerStatic<DashboardRule>()
private const val INTERNAL_PORT = 8080
val OBJECT_MAPPER = ObjectMapper()

class DashboardRule : ExternalResource() {

    private lateinit var koinApp: KoinApplication
    private lateinit var configServiceContainer: KLetsconfigBackend

    private lateinit var okHttp: OkHttpClient

    override fun before() {
        startConfigService()
        startDashboard()
    }

    private fun startConfigService() {
        val logConsumer = Slf4jLogConsumer(LOG)
        configServiceContainer = KLetsconfigBackend("letsconfig-backend:latest")
                .withExposedPorts(INTERNAL_PORT)
        configServiceContainer.start()
        configServiceContainer.followOutput(logConsumer)

    }

    private fun startDashboard() {
        koinApp = startKoin {
            modules(mainModule)
        }.properties(mapOf(
                Pair("config_server.hostname", "localhost"),
                Pair("config_server.port", configServiceContainer.getMappedPort(INTERNAL_PORT)),
                Pair("dashboard.port", 9299),
                Pair("serve.static", false)
        ))
        val server = koinApp.koin.get<JavalinServer>()
        server.start()

        okHttp = OkHttpClient()
    }

    fun <T> executeGetRequest(endpoint: String, responseClass: Class<T>): T {
        val response = okHttp.newCall(Request.Builder().url("http://localhost:9299/api$endpoint").build()).execute()
        response.code shouldBeEqualTo 200
        return response.body?.byteStream().use {
            OBJECT_MAPPER.readValue(it, responseClass)
        }
    }

    fun <T> executePostRequest(endpoint: String, bodyParams: Map<String, String>, responseClass: Class<T>,
                               requestId: String = UUID.randomUUID().toString(), expectedResponseCode: Int = 200): T? {

        val formBody = FormBody.Builder()
        bodyParams.forEach { (key, value) -> formBody.add(key, value) }
        formBody.add("requestId", requestId)
        val response = okHttp.newCall(Request.Builder().url("http://localhost:9299/api$endpoint")
                .post(formBody.build())
                .build()).execute()
        if (response.code != expectedResponseCode) {
            LOG.error("Body = " + response.body?.string())
            Assert.fail()
        }
        if (response.body?.contentLength() == 0L) {
            return null
        }
        return response.body?.byteStream().use {
            OBJECT_MAPPER.readValue(it, responseClass)
        }
    }

    fun updateProperty(applicationName: String, hostName: String, propertyName: String, propertyValue: String, requestId: String) {
        executePostRequest("/property/update", mapOf(
                Pair("applicationName", applicationName),
                Pair("hostName", hostName),
                Pair("propertyName", propertyName),
                Pair("propertyValue", propertyValue)
        ), Map::class.java, requestId)
    }

    fun searchProperties(applicationName: String? = null, hostName: String? = null, propertyName: String? = null,
                         propertyValue: String? = null): List<ShowPropertyItem> {
        val query = StringBuilder()
        if (applicationName != null) {
            query.append("applicationName=$applicationName")
        }
        if (hostName != null) {
            query.append("hostName=$hostName")
        }
        if (propertyName != null) {
            query.append("propertyName=$propertyName")
        }
        if (propertyValue != null) {
            query.append("propertyValue=$propertyValue")
        }
        if (query.isEmpty()){
            throw RuntimeException("Query is empty")
        }
        return executeGetRequest("/property/search?$query", List::class.java).map {
            @Suppress("UNCHECKED_CAST")
            val mapping = it as Map<String, Any>
            ShowPropertyItem(mapping.getValue("applicationName").toString(), mapping.getValue("hostName").toString(),
                    mapping.getValue("propertyName").toString(), mapping.getValue("propertyValue").toString(),
                    (mapping.getValue("version") as Int).toLong())
        }
    }

    override fun after() {
        stopKoin()
        configServiceContainer.stop()
    }
}

private class KLetsconfigBackend(name: String) : GenericContainer<KLetsconfigBackend>(name)
