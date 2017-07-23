package com.letsconfig.config

import com.letsconfig.sdk.server.tokens.Token
import org.skife.jdbi.v2.DBI
import org.skife.jdbi.v2.StatementContext
import java.sql.ResultSet
import java.util.*

/**
 * Date: 15.02.17.
 */
class PropertiesDAO(private val dbi: DBI) {
    fun getValues(activeToken: Token, keys: Collection<String>): Map<String, Property?> {
        val dbValues: MutableMap<String, Property?> = getValuesInner(activeToken, keys)
        keys.forEach {
            if (dbValues[it] == null) {
                dbValues.put(it, null)
            }
        }
        return dbValues
    }

    private fun getValuesInner(activeToken: Token, keys: Collection<String>): MutableMap<String, Property?> {
        if (keys.size == 1) {
            dbi.open().use {
                val value = it.createQuery("select p.id, p.key, p.value, p.updated_datetime from properties p join tokens t on p.token_id = t.id where t.token = :token and p.key = :key")
                        .bind("token", activeToken.token)
                        .bind("key", keys.iterator().next())
                        .map(propertyMapper())
                        .firstOrNull()
                        ?.second
                return if (value == null) {
                    hashMapOf()
                } else {
                    Collections.singletonMap(keys.iterator().next(), value)
                }
            }
        } else {
            dbi.open().use {
                return it.createQuery("select p.id, p.key, p.value, p.updated_datetime from properties p join tokens t on p.token_id = t.id where t.token = :token and p.key = ANY(:key)")
                        .bind("token", activeToken.token)
                        .bind("key", it.connection.createArrayOf("varchar", keys.toTypedArray()))
                        .map(propertyMapper())
                        .toMap()
                        .toMutableMap()
            }
        }
    }

    private fun propertyMapper(): (Int, ResultSet, StatementContext) -> Pair<String, Property> {
        return { _, resultSet, _ ->
            val key = resultSet.getString(2)
            Pair(key, Property(resultSet.getLong(1), key, resultSet.getString(3), resultSet.getLong(4)))
        }
    }

    fun getAll(activeToken: Token): Map<String, String> {
        dbi.open().use {
            return it.createQuery("select p.key, p.value from properties p join tokens t on p.token_id = t.id where t.token = :token")
                    .bind("token", activeToken.token)
                    .map({ _, resultSet, _ ->
                        Pair(resultSet.getString(1), resultSet.getString(2))
                    })
                    .toMap()
        }
    }

    fun insertValue(token: Token, key: String, value: String) {
        val ct = System.currentTimeMillis()
        dbi.open().use {
            it.createStatement("insert into properties (key, value, token_id, created_datetime, updated_datetime) values (:key, :value, :token_id, :created_datetime, :updated_datetime)")
                    .bind("key", key)
                    .bind("value", prepareValue(value))
                    .bind("token_id", token.id)
                    .bind("created_datetime", ct)
                    .bind("updated_datetime", ct)
                    .execute()
        }
    }

    private fun prepareValue(value: String): String {
        return value.trim { it.isWhitespace() || "\n\r".contains(it) }
    }

    fun updateValue(token: Token, key: String, value: String) {
        val ct = System.currentTimeMillis()
        dbi.open().use {
            it.createStatement("update properties set value=:value, updated_datetime=:ct where key = :key and token_id = :token_id")
                    .bind("key", key)
                    .bind("value", prepareValue(value))
                    .bind("token_id", token.id)
                    .bind("ct", ct)
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

    fun findKeys(token: Token, key: String): List<String> {
        dbi.open().use {
            return it.createQuery("select key from properties where key like :key and token_id = :token_id")
                    .bind("key", "%$key%")
                    .bind("token_id", token.id)
                    .map({ _, resultSet, _ ->
                        resultSet.getString(1)
                    })
                    .list()
        }
    }

    fun findValues(token: Token, value: String?): Map<String, String> {
        dbi.open().use {
            return it.createQuery("select key, value from properties where value like :value and token_id = :token_id")
                    .bind("value", "%$value%")
                    .bind("token_id", token.id)
                    .map({ _, resultSet, _ ->
                        Pair(resultSet.getString(1), resultSet.getString(2))
                    })
                    .toMap()
        }
    }

    fun <K, V> Map<K, V>.toMutableMap(): MutableMap<K, V> {
        return HashMap(this)
    }
}
