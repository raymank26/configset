package com.configset.server.db.common

data class PersistResult<T>(val persistRequestId: Boolean, val res: T)
