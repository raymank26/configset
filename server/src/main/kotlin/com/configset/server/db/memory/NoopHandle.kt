package com.configset.server.db.memory

import com.configset.server.db.common.DbHandle

object NoopHandle : DbHandle {
    override fun <T> getApi(clazz: Class<T>): T {
        throw NotImplementedError("Method should not be called")
    }
}