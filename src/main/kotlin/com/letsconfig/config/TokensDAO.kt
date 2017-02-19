package com.letsconfig.config

import org.skife.jdbi.v2.DBI


/**
 * @author anton.ermak
 *         Date: 15.02.17.
 */
class TokensDAO(private val dbi: DBI) {

    fun getActiveToken(key: String): Token? {
        val handle = dbi.open()
        handle.use { handle ->
            return handle.createQuery("select t.id, t.token from tokens t join users u on t.user_id = u.id where t.token = :key")
                    .bind("key", key)
                    .map({ rowIndex, resultSet, statementContext ->
                        Token(resultSet.getLong(1), resultSet.getString(2))
                    })
                    .firstOrNull()
        }
    }
}
