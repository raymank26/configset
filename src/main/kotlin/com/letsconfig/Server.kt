package com.letsconfig

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.letsconfig.config.PropertiesDAO
import com.letsconfig.config.Property
import com.letsconfig.sdk.ExceptionCode
import com.letsconfig.sdk.ExceptionCode.*
import com.letsconfig.sdk.HttpExecutor
import com.letsconfig.sdk.PropertyData
import com.letsconfig.sdk.server.tokens.Token
import com.letsconfig.sdk.server.tokens.TokensService
import io.prometheus.client.Counter
import org.slf4j.LoggerFactory
import spark.Request
import spark.Response
import spark.Service
import java.util.*

class Server(val tokensService: TokensService, val propertiesService: PropertiesDAO, val settingsApiWebSocket: SettingsApiWebSocket) {

    companion object {
        val objectMapper: ObjectMapper = ObjectMapper()
                .setDefaultPrettyPrinter(DefaultPrettyPrinter())
                .enable(SerializationFeature.INDENT_OUTPUT)
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val requestCounter: Counter = Counter.build()
            .name("total_requests")
            .help("total_requests")
            .labelNames("requestUrl", "method", "clientVersion")
            .register()

    private val exceptionCounter: Counter = Counter.build()
            .name("exceptions")
            .help("Total exception counter")
            .labelNames("name")
            .register()


    fun start(port: Int) {
        val server = Service.ignite()
        server.port(port)
        server.webSocket("/api/v1/websocket/properties", settingsApiWebSocket)

        server.before({ request, response ->
            if (log.isDebugEnabled) {
                log.debug("Request is: params=${request.params()}," +
                        "queryMap=${request.queryMap().toMap()}," +
                        "url=${request.url()}," +
                        "method=${request.requestMethod()}," +
                        "header=[${request.headers().map { "$it=${request.headers(it)}" }.joinToString(",")}]")
            }
            val clientVersion = request.headers(HttpExecutor.VERSION_HEADER)

            if (clientVersion == null) {
                log.warn("Unable to get version");
                server.halt(400, toJson(mapOf(Pair(HttpExecutor.ERROR_FIELD, ExceptionCode.UNKNOWN.code))))
            }
            requestCounter.labels(request.url(), request.requestMethod(), clientVersion).inc()
            if (!request.url().contains("websocket")) {
                val tokenFromHeader = request.headers(HttpExecutor.TOKEN_HEADER)
                if (tokenFromHeader.isNullOrEmpty()) {
                    log.debug("Token not found")
                    server.halt(400, toJson(mapOf(Pair(HttpExecutor.ERROR_FIELD, ExceptionCode.NO_ACTIVE_TOKEN_FOUND.code))))
                } else {
                    val activeToken = tokensService.getActiveToken(tokenFromHeader)
                    if (activeToken == null) {
                        log.debug("No active token found")
                        server.halt(400, toJson(mapOf(Pair(HttpExecutor.ERROR_FIELD, NO_ACTIVE_TOKEN_FOUND.code))))
                    } else {
                        request.attribute(HttpExecutor.TOKEN_HEADER, activeToken)
                    }
                }
            }
        })

        server.get("/api/v1/conf", { req, res ->
            val keys = req.queryParamsValues("keys")
            if (keys == null) {
                server.halt(200, "{}")
            }
            val token = getActiveToken(req)

            val value: Map<String, Property?> = propertiesService.getValues(token, keys.toList())
            if (value.isEmpty()) {
                server.halt(400, toJson(mapOf(Pair(HttpExecutor.ERROR_FIELD, NO_SUCH_ELEMENT.code))))
            } else {
                toJson(value.mapValues { it.value?.let { PropertyData(it.value, it.update_time) } })
            }
        })

        server.get("/api/v1/conf/all", { req: Request, res: Response ->
            val token = getActiveToken(req)
            val mapping = propertiesService.getAll(token)
            toJson(mapping)
        })

        server.post("/api/v1/conf", { req, res ->
            val key = req.raw().getParameter("key")
            val token = getActiveToken(req)
            val value = req.raw().getParameter("value")
            if (value == null) {
                server.halt(400, toJson(mapOf(Pair(HttpExecutor.ERROR_FIELD, NO_VALUE_FOUND.code))))
            } else {
                // TODO: move to SQL locking
                synchronized(this) {
                    when (propertiesService.getValues(token, Collections.singletonList(key))[key] == null) {
                        true -> propertiesService.insertValue(token, key, value)
                        false -> propertiesService.updateValue(token, key, value)
                    }
                }
                ""
            }
        })

        server.delete("api/v1/conf/:key", { req, res ->
            val key = req.params("key")
            val token = getActiveToken(req)
            propertiesService.delete(token, key)
            ""
        })

        server.get("/api/v1/find/key", { req, res ->
            val key = req.queryParams("key")
            val token = getActiveToken(req)
            toJson(hashMapOf(Pair("keys", propertiesService.findKeys(token, key))))
        })
        server.get("/api/v1/find/value", { req, res ->
            val value = req.queryParams("value")
            val token = getActiveToken(req)
            toJson(hashMapOf(Pair("properties", propertiesService.findValues(token, value))))
        })

        server.exception(Exception::class.java, { exception, request, response ->
            log.error("Exception catched while request processing", exception)
            exceptionCounter.labels(exception.javaClass.simpleName).inc()
            response.status(500)
            response.body(objectMapper.writeValueAsString(Collections.singletonMap(HttpExecutor.ERROR_FIELD, UNKNOWN.code)))
        })

        log.info("Server started on port $port ...")
    }

    private fun getActiveToken(req: Request): Token {
        val token = req.attribute<Token>(HttpExecutor.TOKEN_HEADER)
        if (token == null) {
            throw IllegalStateException("No token found")
        } else {
            return token
        }
    }

    private fun toJson(value: Any): String {
        return objectMapper.writeValueAsString(value)
    }
}

