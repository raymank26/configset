package com.configset.server.db.memory

import com.configset.server.db.DbHandleFactory
import com.configset.server.db.common.DbHandle

object InMemoryDbHandleFactory : DbHandleFactory {
    override fun <T> withHandle(f: (DbHandle) -> T): T {
        return f(NoopHandle)
    }
}