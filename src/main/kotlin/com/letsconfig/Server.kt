package com.letsconfig

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.letsconfig.config.PropertiesDAO
import com.letsconfig.config.Token
import com.letsconfig.config.TokensService
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
        val tokenKey = "X-Token"

        private val NO_TOKEN_FOUND = "1000"
        private val NO_SUCH_ELEMENT = "1001"
        private val NO_VALUE_FOUND = "1002"
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val requestCounter: Counter = Counter.build()
            .name("total_requests")
            .help("total_requests")
            .labelNames("requestUrl")
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
            requestCounter.labels(request.url()).inc()
            val tokenFromHeader = request.headers(tokenKey)
            if (tokenFromHeader.isNullOrEmpty()) {
                server.halt(400, toJson(mapOf(Pair("error", NO_TOKEN_FOUND))))
            } else {
                val activeToken = tokensService.getActiveToken(tokenFromHeader)
                if (activeToken == null) {
                    server.halt(400, toJson(mapOf(Pair("error", NO_TOKEN_FOUND))))
                } else {
                    request.attribute(tokenKey, activeToken)
                }
            }
        })

        server.get("/api/v1/conf", { req, res ->
            val keys = req.queryParamsValues("keys")
            if (keys == null) {
                server.halt(200, "{}")
            }
            val token = getActiveToken(req)

            val value = propertiesService.getValues(token, keys.toList())
            if (value.isEmpty()) {
                server.halt(400, toJson(mapOf(Pair("error", NO_SUCH_ELEMENT))))
            } else {
                toJson(value)
            }
        })

        server.get("/api/v1/conf/all", { req: Request, res: Response ->
            val token = getActiveToken(req)
            val mapping = propertiesService.getAll(token)
            toJson(mapping)
        })

        server.post("/api/v1/conf", { req, res ->
            val keys = req.raw().getParameter("key")
            val token = getActiveToken(req)
            val value = req.raw().getParameter("value")
            if (value == null) {
                server.halt(400, toJson(mapOf(Pair("error", NO_VALUE_FOUND))))
            } else {
                // TODO: move to SQL locking
                synchronized(this) {
                    when (propertiesService.getValues(token, Collections.singletonList(keys)).isEmpty()) {
                        true -> propertiesService.insertValue(token, keys, value)
                        false -> propertiesService.updateValue(token, keys, value)
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

        log.info("Server started on port $port ...")
    }

    private fun getActiveToken(req: Request): Token {
        val token = req.attribute<Token>(tokenKey)
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

