package com.configset.dashboard.util

import org.apache.commons.codec.digest.DigestUtils
import java.util.UUID

class RequestIdProducer {

    fun nextRequestId(requestId: String): String {
        return DigestUtils.md5Hex(requestId)
    }

    fun nextRequestId(): String {
        return UUID.randomUUID().toString()
    }
}