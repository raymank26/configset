package com.configset.server.db

import com.configset.server.db.common.DbHandle

interface RequestIdDao {
    fun exists(handle: DbHandle, requestId: String): Boolean
    fun persist(handle: DbHandle, requestId: String)
}