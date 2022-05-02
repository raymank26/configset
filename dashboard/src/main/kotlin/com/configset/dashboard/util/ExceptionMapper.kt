package com.configset.dashboard.util

import com.configset.sdk.extension.createLoggerStatic
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.javalin.Javalin
import org.eclipse.jetty.http.HttpStatus

private val LOG = createLoggerStatic<ExceptionMapper>()

class ExceptionMapper {
    fun bind(app: Javalin) {
        app.exception(BadRequest::class.java) { e, ctx ->
            ctx.status(400)
            ctx.json(ServerExceptionResponse(e.code, e.details))
        }
        app.exception(PermissionDenied::class.java) { e, ctx ->
            ctx.status(HttpStatus.FORBIDDEN_403)
        }
        app.exception(StatusRuntimeException::class.java) { e, ctx ->
            if (e.status.code == Status.Code.UNAUTHENTICATED) {
                ctx.status(HttpStatus.FORBIDDEN_403)
            } else {
                LOG.warn("Unknown gRPC exception", e)
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
            }
        }
    }
}

private data class ServerExceptionResponse(val code: String, val details: String?)
