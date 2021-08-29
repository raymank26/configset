package com.configset.server.db.postgres

import com.configset.server.db.common.DbHandle
import org.jdbi.v3.core.Handle

class PostgresDbHandle(private val handle: Handle) : DbHandle {
    override fun <T> getApi(clazz: Class<T>): T {
        return handle.attach(clazz)
    }
}