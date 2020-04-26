package com.letsconfig.dashboard.util

class BadRequest(val code: String, val details: String? = null) : Exception()
