package com.configset.server.db.postgres

import java.sql.ResultSet

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

}