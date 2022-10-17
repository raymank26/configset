package com.configset.server.db.postgres

import com.configset.server.db.common.DbHandle
import org.jdbi.v3.core.Handle

class PostgresDbHandle(val handle: Handle) : DbHandle


fun <T, R> DbHandle.withApi(cls: Class<T>, lambda: (T) -> R): R {
    return asDbi().attach(cls).let(lambda)
}

fun <R> DbHandle.inTransaction(lambda: (Handle) -> R): R {
    return asDbi().inTransaction<R, Exception> { lambda(it) }
}

fun DbHandle.asDbi(): Handle {
    return (this as PostgresDbHandle).handle
}


