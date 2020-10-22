package com.configset.dashboard.util

import org.apache.commons.codec.digest.DigestUtils

class RequestIdProducer {

    fun nextRequestId(requestId: String): String {
        return DigestUtils.md5Hex(requestId)
    }
}