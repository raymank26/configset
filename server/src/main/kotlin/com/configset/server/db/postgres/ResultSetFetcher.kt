package com.configset.server.db.postgres

import java.sql.ResultSet

class ResultSetFetcher(
    resultSetPrefixFetcherBuilder: ResultSetPrefixFetcherBuilder,
    private val rs: ResultSet,
) {

    private val prefixMapping = resultSetPrefixFetcherBuilder.prefixMapping

    fun getLong(name: String): Long {
        return rs.getLong(prefixMapping[name]!!)
    }

    fun getBoolean(name: String): Boolean {
        return rs.getBoolean(prefixMapping[name]!!)
    }

    fun getString(name: String): String {
        return rs.getString(prefixMapping[name]!!)
    }
}
