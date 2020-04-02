package com.letsconfig

import org.jdbi.v3.core.Jdbi

/**
 * Date: 15.02.17.
 */

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val dbi = Jdbi.create("jdbc:postgresql://10.8.0.1:5432/letsconfig?connectTimeout=5", "letsconfig", "tahA5qajrQEu4S2e")
//        val tokensDao = TokensDAO(dbi)
//        val tokenService = TokensService(tokensDao)
//        val propertiesDAO = PropertiesDAO(dbi)
//
//        val settingsApiWebSocket = SettingsApiWebSocket(propertiesDAO, tokenService)
//        settingsApiWebSocket.start()
//        val server = Server(tokenService, propertiesDAO, settingsApiWebSocket)
//        server.start(8080)
//        startMetricServer()
    }
}
