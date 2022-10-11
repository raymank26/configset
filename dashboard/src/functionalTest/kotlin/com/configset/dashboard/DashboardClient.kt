package com.configset.dashboard

import arrow.core.Either
import arrow.core.left
import com.configset.sdk.extension.createLoggerStatic
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID

private val LOG = createLoggerStatic<DashboardClient>()

val OBJECT_MAPPER = ObjectMapper()

class DashboardClient {

    private val okHttp = OkHttpClient().newBuilder()
        .followRedirects(false)
        .build()

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
            .header("Cookie", "auth.access_token=${AccessTokenTestUtils.createAccessToken()}")
    }

    fun <T> executeRequest(request: Request, responseClass: JavaType): Either<DashboardHttpFailure, T?> {
        val response = okHttp.newCall(request)
            .execute()
        if (response.code == 404) {
            return DashboardHttpFailure(400, "Not found").left()
        }
        if (response.code == 302) {
            return DashboardHttpFailure(403, "Auth required").left()
        }
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

    fun getProperty(
        appName: String,
        hostName: String,
        propertyName: String,
    ): Either<DashboardHttpFailure, ShowPropertyItem?> {
        return executeGetRequest(
            "/property/get",
            OBJECT_MAPPER.typeFactory.constructType(ShowPropertyItem::class.java),
            mapOf(
                Pair("applicationName", appName),
                Pair("hostName", hostName),
                Pair("propertyName", propertyName)
            )
        )
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
    ): Either<DashboardHttpFailure, List<TablePropertyItem>> {
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
            .constructCollectionType(ArrayList::class.java, TablePropertyItem::class.java)
        return executeGetRequest<List<TablePropertyItem>>("/property/search", responseClass, queryParams)
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
        val response = okHttp.newCall(
            Request.Builder().url("http://localhost:9299/api$endpoint")
                .header("Cookie", "auth.access_token=${AccessTokenTestUtils.createAccessToken()}")
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
}

data class DashboardHttpFailure(
    val httpCode: Int,
    val errorCode: String?,
)