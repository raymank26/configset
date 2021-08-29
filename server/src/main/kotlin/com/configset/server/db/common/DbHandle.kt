package com.configset.server.db.common

interface DbHandle {
    fun <T> getApi(clazz: Class<T>): T
}