package com.letsconfig.server.db.common


fun containsLowerCase(value: String, template: String): Boolean {
    return value.toLowerCase().contains(template.toLowerCase())
}
