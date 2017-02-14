package com.letsconfig

import com.letsconfig.config.PropertiesService
import com.letsconfig.config.Token
import com.letsconfig.config.TokensService
import org.slf4j.LoggerFactory
import spark.Request
import spark.Service

class Server(val tokensService: TokensService, val propertiesService: PropertiesService) {

    val log = LoggerFactory.getLogger(javaClass)
    val tokenKey = "Token"

    fun start(port: Int) {
        val server = Service.ignite()
        server.port(port)
        server.before({ request, response ->
            val tokenKey = request.headers(tokenKey)
            if (tokenKey.isNullOrEmpty()) {
                response.status(400)
                response.body("Token is empty");
            } else {
                val activeToken = tokensService.getActiveToken(tokenKey)
                if (activeToken == null) {
                    response.status(400)
                    response.body("No active token found")
                } else {
                    request.attribute(tokenKey, activeToken)
                }
            }
            request.attribute(this.tokenKey, tokenKey)
        })

        server.get("/api/v1/conf/:key") { req, res ->
            {
                val key = req.params("key")
                val token = getActiveToken(req)

                val value = propertiesService.getValue(token, key)
                if (value == null) {
                    res.status(400)
                    res.body("No value found for received key")
                } else {
                    res.body(value)
                }
            }
        }

        server.get("/api/v1/conf") { req, res ->
            {
                val token = getActiveToken(req)
                val mapping = propertiesService.getAll(token)
                res.body(toJson(mapping))
            }
        }

        server.post("/api/v1/:key") { req, res ->
            {
                val key = req.params("key")
                val token = getActiveToken(req)
                val value = req.body()

                propertiesService.setValue(token, key, value)
            }
        }

        server.delete("api/v1/:key", { req, res ->
            {
                val key = req.params("key")
                val token = getActiveToken(req)
                propertiesService.delete(token, key)
            }
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
        TODO()
    }
}

