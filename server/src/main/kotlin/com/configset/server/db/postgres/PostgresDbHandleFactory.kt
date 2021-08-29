package com.configset.server.db.postgres

import com.configset.server.db.DbHandleFactory
import com.configset.server.db.common.DbHandle
import org.jdbi.v3.core.Jdbi

class PostgresDbHandleFactory(private val dbi: Jdbi) : DbHandleFactory {
    override fun <T> withHandle(f: (DbHandle) -> T): T {
        return dbi.inTransaction<T, Exception> {
            f(PostgresDbHandle(it))
        }
    }
}
