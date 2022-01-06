package com.configset.dashboard

import com.configset.sdk.extension.createLoggerStatic
import com.configset.test.fixtures.ACCESS_TOKEN
import com.configset.test.fixtures.SERVER_PORT
import com.configset.test.fixtures.ServiceStarterRule
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Assert
import org.junit.rules.ExternalResource
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import java.util.*

private val LOG = createLoggerStatic<DashboardRule>()
private val OBJECT_MAPPER = ObjectMapper()

class DashboardRule : ExternalResource() {

    private lateinit var koinApp: KoinApplication
    private val serverStarterRule = ServiceStarterRule()

    private lateinit var okHttp: OkHttpClient

    override fun before() {
        startConfigService()
        startDashboard()
    }

    private fun startConfigService() {
        serverStarterRule.before()
    }

    private fun startDashboard() {
        koinApp = koinApplication {
            modules(mainModule)
        }.properties(mapOf(
            Pair("config", Config(
                mapOf(
                    Pair("config_server.hostname", "localhost"),
                    Pair("config_server.port", SERVER_PORT.toString()),
                    Pair("dashboard.port", 9299.toString()),
                    Pair("serve.static", "false"),
                    Pair("authenticator_type", ""),
                    Pair("client.keycloackUrl", "localhost"),
                    Pair("client.keycloackRealm", "sample-realm"),
                    Pair("client.keycloackClientId", "sample-clientId"),
                )
            ))
        ))
        val server = koinApp.koin.get<JavalinServer>()
        server.start()

        okHttp = OkHttpClient()
    }

    fun <T> executeGetRequest(
        endpoint: String,
        responseClass: Class<T>,
        queryParams: Map<String, String> = emptyMap(),
    ): T {
        val urlBuilder = HttpUrl.Builder()
            .scheme("http")
            .host("localhost")
            .port(9299)
            .addPathSegments("api$endpoint")
        for ((key, value) in queryParams) {
            urlBuilder.addQueryParameter(key, value)
        }
        val response = okHttp.newCall(Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer $ACCESS_TOKEN")
            .build())
            .execute()
        response.code shouldBeEqualTo 200
        return response.body?.byteStream().use {
            OBJECT_MAPPER.readValue(it, responseClass)
        }
    }

    fun <T> executePostRequest(
        endpoint: String, bodyParams: Map<String, String>, responseClass: Class<T>,
        requestId: String = UUID.randomUUID().toString(), expectedResponseCode: Int = 200,
    ): T? {

        val formBody = FormBody.Builder()
        bodyParams.forEach { (key, value) -> formBody.add(key, value) }
        formBody.add("requestId", requestId)
        val response = okHttp.newCall(Request.Builder().url("http://localhost:9299/api$endpoint")
            .header("Authorization", "Bearer $ACCESS_TOKEN")
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

    fun updateProperty(
        applicationName: String,
        hostName: String,
        propertyName: String,
        propertyValue: String,
        requestId: String,
    ) {
        executePostRequest("/property/update", mapOf(
            Pair("applicationName", applicationName),
            Pair("hostName", hostName),
            Pair("propertyName", propertyName),
            Pair("propertyValue", propertyValue)
        ), Map::class.java, requestId)
    }

    fun searchProperties(
        applicationName: String? = null, hostName: String? = null, propertyName: String? = null,
        propertyValue: String? = null,
    ): List<ShowPropertyItem> {
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
        return executeGetRequest("/property/search", List::class.java, queryParams).map {
            @Suppress("UNCHECKED_CAST")
            val mapping = it as Map<String, Any>
            ShowPropertyItem(mapping.getValue("applicationName").toString(), mapping.getValue("hostName").toString(),
                mapping.getValue("propertyName").toString(), mapping.getValue("propertyValue").toString(),
                (mapping.getValue("version") as Int).toLong())
        }
    }

    override fun after() {
        koinApp.close()
        serverStarterRule.after()
    }
}
