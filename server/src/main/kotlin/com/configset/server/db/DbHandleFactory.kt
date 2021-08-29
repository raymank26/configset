package com.configset.server.db

import com.configset.server.db.common.DbHandle

interface DbHandleFactory {
    fun <T> withHandle(f: (DbHandle) -> T): T
}
