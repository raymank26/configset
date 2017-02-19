package com.letsconfig

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.letsconfig.config.PropertiesDAO
import com.letsconfig.config.Token
import com.letsconfig.config.TokensService
import org.slf4j.LoggerFactory
import spark.Request
import spark.Response
import spark.Service

class Server(val tokensService: TokensService, val propertiesService: PropertiesDAO) {

    companion object {
        private val NO_TOKEN_FOUND = "1000"
        private val NO_SUCH_ELEMENT = "1001"
        private val NO_VALUE_FOUND = "1002"
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val tokenKey = "Token"
    private val objectMapper = ObjectMapper()
            .setDefaultPrettyPrinter(DefaultPrettyPrinter())
            .enable(SerializationFeature.INDENT_OUTPUT)

    fun start(port: Int) {
        val server = Service.ignite()
        server.port(port)

        server.before({ request, response ->
            if (log.isDebugEnabled) {
                log.debug("Request is: params=${request.params()}, queryMap=${request.queryMap().toMap()}")
            }
            val tokenFromHeader = request.headers(tokenKey)
            if (tokenFromHeader.isNullOrEmpty()) {
                response.status(400)
                response.body(toJson(mapOf(Pair("error", NO_TOKEN_FOUND))))
            } else {
                val activeToken = tokensService.getActiveToken(tokenFromHeader)
                if (activeToken == null) {
                    response.status(400)
                    response.body(toJson(mapOf(Pair("error", NO_TOKEN_FOUND))))
                } else {
                    request.attribute(this.tokenKey, activeToken)
                }
            }
        })

        server.get("/api/v1/conf/:key", { req, res ->
                val key = req.params("key")
                val token = getActiveToken(req)

                val value = propertiesService.getValue(token, key)
                if (value == null) {
                    res.status(400)
                    toJson(mapOf(Pair("error", NO_SUCH_ELEMENT)))
                } else {
                    value
                }
        })

        server.get("/api/v1/conf", { req: Request, res: Response ->
            val token = getActiveToken(req)
            val mapping = propertiesService.getAll(token)
            toJson(mapping)
        })

        server.post("/api/v1/conf/:key", { req, res ->
            val key = req.params("key")
            val token = getActiveToken(req)
            val value = req.queryMap().get("value").value()
            if (value == null) {
                res.status(400)
                toJson(mapOf(Pair("error", NO_VALUE_FOUND)))
            } else {
                // TODO: move to SQL locking
                synchronized(this) {
                    when (propertiesService.getValue(token, key)) {
                        null -> propertiesService.insertValue(token, key, value)
                        value -> Unit
                        else -> propertiesService.updateValue(token, key, value)
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

    private fun toJson(mapping: Map<String, String>): String {
        return objectMapper.writeValueAsString(mapping)
    }
}

