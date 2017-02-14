package com.letsconfig

import com.letsconfig.config.PropertiesService
import com.letsconfig.config.TokensDAO
import com.letsconfig.config.TokensService

/**
 * @author anton.ermak
 *         Date: 15.02.17.
 */

object Main {
    @JvmStatic fun main(args: Array<String>) {
        val tokensDao = TokensDAO()
        val tokenService = TokensService(tokensDao)
        val propertiesService = PropertiesService()

        val server = Server(tokenService, propertiesService)
        server.start(8080)
    }
}
