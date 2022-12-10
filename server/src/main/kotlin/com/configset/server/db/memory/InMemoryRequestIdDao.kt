package com.configset.server.db.memory

import com.configset.server.db.RequestIdDao
import com.configset.server.db.common.DbHandle

class InMemoryRequestIdDao : RequestIdDao {

    private val processed: MutableSet<String> = hashSetOf()

    override fun exists(handle: DbHandle, requestId: String): Boolean {
        return processed.contains(requestId)
    }

    override fun persist(handle: DbHandle, requestId: String) {
        processed.add(requestId)
    }
}
