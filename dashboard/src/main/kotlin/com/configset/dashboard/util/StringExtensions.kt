package com.configset.dashboard.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun String.urlEncode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8)
}
