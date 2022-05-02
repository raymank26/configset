package com.configset.dashboard.util

import io.javalin.http.Context

fun Context.requestId() = this.formParam("requestId") ?: throw BadRequest("requestId")

fun Context.accessToken() = this.header("Authorization")?.split(" ")?.getOrNull(1)
    ?: error("No Authentication header found")

fun Context.formParamSafe(name: String) = this.formParam(name) ?: throw BadRequest("param.not.found", name)

fun Context.queryParamSafe(name: String) = this.queryParam(name) ?: throw BadRequest("queryParam.not.found", name)
