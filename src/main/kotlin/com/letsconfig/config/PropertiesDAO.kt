package com.letsconfig.config

import org.skife.jdbi.v2.DBI

/**
 * @author anton.ermak
 *         Date: 15.02.17.
 */
class PropertiesDAO(private val dbi: DBI) {
    fun getValue(activeToken: Token, key: String): String? {
        dbi.open().use {
            return it.createQuery("select p.value from properties p join tokens t on p.token_id = t.id where t.token = :token and p.key = :key")
                    .bind("token", activeToken.token)
                    .bind("key", key)
                    .map({ row, resultSet, statementContext ->
                        resultSet.getString(1)
                    })
                    .firstOrNull()
        }
    }

    fun getAll(activeToken: Token): Map<String, String> {
        dbi.open().use {
            return it.createQuery("select p.key, p.value from properties p join tokens t on p.token_id = t.id where t.token = :token")
                    .bind("token", activeToken.token)
                    .map({ row, resultSet, statementContext ->
                        Pair(resultSet.getString(1), resultSet.getString(2))
                    })
                    .toMap()
        }
    }

    fun insertValue(token: Token, key: String, value: String) {
        dbi.open().use {
            it.createStatement("insert into properties (key, value, token_id) values (:key, :value, :token_id)")
                    .bind("key", key)
                    .bind("value", value)
                    .bind("token_id", token.id)
                    .execute()
        }
    }

    fun updateValue(token: Token, key: String, value: String) {
        dbi.open().use {
            it.createStatement("update properties set value=:value where key = :key and token_id = :token_id")
                    .bind("key", key)
                    .bind("value", value)
                    .bind("token_id", token.id)
                    .execute()
        }
    }

    fun delete(token: Token, key: String) {
        dbi.open().use {
            it.createStatement("delete from properties where key = :key and token_id = :token_id")
                    .bind("key", key)
                    .bind("token_id", token.id)
                    .execute()
        }

    }
}
