package com.configset.dashboard.util

class BadRequest(val code: String, val details: String? = null) : Exception()
