package com.configset.dashboard.util

import org.apache.commons.lang3.StringEscapeUtils


fun String.escapeHtml(): String {
    return StringEscapeUtils.escapeHtml4(this)
}