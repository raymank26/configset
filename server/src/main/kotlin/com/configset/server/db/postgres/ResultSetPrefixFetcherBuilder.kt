package com.configset.server.db.postgres

import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap


class ResultSetPrefixFetcherBuilder(rs: ResultSet) {

    val prefixMapping = mutableMapOf<String, Int>()

    init {
        var prefix = ""
        for (i in 1..rs.metaData.columnCount) {
            if (rs.metaData.getColumnName(i).startsWith("table_")) {
                prefix = rs.metaData.getColumnName(i).split("_")[1]
                continue
            }
            require(prefix != "")
            prefixMapping["${prefix}.${rs.metaData.getColumnName(i)}"] = i
        }
    }

    fun getFetcher(rs: ResultSet): ResultSetFetcher {
        return ResultSetFetcher(this, rs)
    }

    companion object {

        private val registry = ConcurrentHashMap<Class<*>, ResultSetPrefixFetcherBuilder>()

        fun getFetcher(cls: Class<*>, rs: ResultSet): ResultSetFetcher {
            return registry.computeIfAbsent(cls) {
                ResultSetPrefixFetcherBuilder(rs)
            }.getFetcher(rs)
        }

        // '' as table_cp, cp.*, '' as table_ch, ch.*, '' as table_ca, ca.*
        fun buildSelectExp(aliases: List<String>): String {
            val parts = mutableListOf<String>()
            for (alias in aliases) {
                parts.add("'' as table_${alias}, ${alias}.*")
            }
            return parts.joinToString(", ")
        }
    }
}