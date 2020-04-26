package com.letsconfig.dashboard.util

import java.security.MessageDigest

class RequestIdProducer {

    fun nextRequestId(requestId: String): String {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(requestId.toByteArray())
        return String(digest.digest())
    }
}