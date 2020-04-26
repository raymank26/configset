package com.letsconfig.dashboard.util

import io.javalin.Javalin

class ExceptionMapper {

    fun bind(app: Javalin) {
        app.exception(BadRequest::class.java) { e, ctx ->
            ctx.status(400)
            ctx.json(ServerExceptionResponse(e.code, e.details))
        }
    }
}

private data class ServerExceptionResponse(val code: String, val details: String?)
