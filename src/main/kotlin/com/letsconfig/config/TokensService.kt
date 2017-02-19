package com.letsconfig.config

/**
 * @author anton.ermak
 *         Date: 15.02.17.
 */
class TokensService(val tokensDAO : TokensDAO) {
    fun getActiveToken(tokenKey: String): Token? {
        return tokensDAO.getActiveToken(tokenKey)
    }
}
