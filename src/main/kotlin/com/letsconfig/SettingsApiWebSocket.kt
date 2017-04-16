package com.letsconfig

import com.fasterxml.jackson.core.type.TypeReference
import com.letsconfig.config.PropertiesDAO
import com.letsconfig.sdk.HttpExecutor
import com.letsconfig.sdk.PropertyData
import com.letsconfig.sdk.server.tokens.Token
import com.letsconfig.sdk.server.tokens.TokensService
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Date: 01.03.17.
 */
@WebSocket
class SettingsApiWebSocket(private val propertiesDAO: PropertiesDAO, private val tokensService: TokensService) {

    private val log = LoggerFactory.getLogger(this.javaClass)
    private val protocolOutputVersion = 1

    private val sessions: ConcurrentHashMap<Session, PropertiesContext> = ConcurrentHashMap()
    private val executor = Executors.newSingleThreadScheduledExecutor()

    fun start() {
        executor.scheduleWithFixedDelay(update(), 0, 10, TimeUnit.SECONDS)
    }

    private fun update(): Runnable = Runnable {
        log.debug("Update started..")
        sessions.forEach { session, settings ->
            try {
                sendProperties(session, settings)
            } catch (e: Exception) {
                log.warn("Unable to send response to " + session.remoteAddress)
            }
        }
    }

    private fun sendProperties(session: Session, propertiesContext: PropertiesContext) {
        val values: Map<String, PropertyData?> = propertiesDAO.getValues(propertiesContext.token, propertiesContext.keys).mapValues {
            it.value?.let {
                PropertyData(it.value, it.update_time)
            }
        }
        session.remote.sendBytes(ByteBuffer.wrap(Server.objectMapper.writeValueAsBytes(values)))
    }

    @OnWebSocketConnect
    fun onSocketConnect(session: Session) {
        log.debug("Session {} connected to me", session)
    }

    @OnWebSocketClose
    fun onSocketClose(session: Session, closeCode: Int, closeReason: String) {
        log.debug("Session {} closed with code = {} and reason = {}", session, closeCode, closeReason)
        sessions.remove(session)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, buf: ByteArray, offset: Int, length: Int) {
        val activeToken = session.upgradeRequest.headers.get(HttpExecutor.TOKEN_HEADER)?.let {
            tokensService.getActiveToken(it[0])
        }
        log.debug("Session {} send messages with offset = {}, length = {}, activeToken = {}", session, offset, length,
                activeToken)

        if (activeToken == null) {
            log.warn("Active token doesn't exist. Skipping...")
        } else {
            val typeRef = object : TypeReference<Set<String>>() {}
            val propertiesKeys: Set<String> = Server.objectMapper.readValue(ByteArrayInputStream(buf, offset, length), typeRef)
            log.debug("Property keys received = {}", propertiesKeys)
            val value = PropertiesContext(activeToken, propertiesKeys)
            sessions.put(session, value)
            sendProperties(session, value)
        }
    }

    private fun prepareMessage(properties: Map<String, String>): ByteArray {
        val bos = ByteArrayOutputStream()
        val os = ObjectOutputStream(bos)
        os.writeObject(protocolOutputVersion)
        os.writeObject(properties)
        os.close()
        return bos.toByteArray()
    }
}

private data class PropertiesContext(val token: Token, val keys: Set<String>)
