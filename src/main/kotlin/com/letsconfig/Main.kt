package com.letsconfig

import com.letsconfig.config.PropertiesDAO
import com.letsconfig.config.TokensDAO
import com.letsconfig.config.TokensService
import org.skife.jdbi.v2.DBI

/**
 * @author anton.ermak
 *         Date: 15.02.17.
 */

object Main {
    @JvmStatic fun main(args: Array<String>) {
        val dbi = DBI("jdbc:postgresql://10.9.0.1:5432/letsconfig?connectTimeout=5", "postgres", "")
        val tokensDao = TokensDAO(dbi)
        val tokenService = TokensService(tokensDao)
        val propertiesService = PropertiesDAO(dbi)

        val server = Server(tokenService, propertiesService)
        server.start(8080)
    }
}
