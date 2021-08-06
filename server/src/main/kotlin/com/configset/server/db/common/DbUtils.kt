package com.configset.server.db.common

import java.util.*


fun containsLowerCase(value: String, template: String): Boolean {
    return value.lowercase(Locale.getDefault()).contains(template.lowercase(Locale.getDefault()))
}
