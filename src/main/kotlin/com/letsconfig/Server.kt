package com.letsconfig

import com.fasterxml.jackson.databind.ObjectMapper
import com.letsconfig.config.PropertiesDAO
import com.letsconfig.config.Token
import com.letsconfig.config.TokensService
import org.slf4j.LoggerFactory
import spark.Request
import spark.Response
import spark.Service

class Server(val tokensService: TokensService, val propertiesService: PropertiesDAO) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val tokenKey = "Token"
    private val objectMapper = ObjectMapper()

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
                response.body("Token is empty")
            } else {
                val activeToken = tokensService.getActiveToken(tokenFromHeader)
                if (activeToken == null) {
                    response.status(400)
                    response.body("No active token found")
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
                    "No value found for received key"
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
                "No 'value' specified"
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

