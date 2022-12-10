package com.configset.dashboard.util

import com.configset.common.client.extension.createLoggerStatic
import com.configset.dashboard.ServerApiGatewayErrorType
import com.configset.dashboard.ServerApiGatewayException
import com.configset.dashboard.property.ImportErrorType
import com.configset.dashboard.property.ImportPropertiesException
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.javalin.Javalin
import io.javalin.http.NotFoundResponse
import org.eclipse.jetty.http.HttpStatus

private val LOG = createLoggerStatic<JavalinExceptionMapper>()

class JavalinExceptionMapper {
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
                ctx.htmxShowAlert("Server error")
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
            }
        }

        app.exception(ServerApiGatewayException::class.java) { e, ctx ->
            ctx.status(400)
            ctx.json(
                ServerExceptionResponse(
                    when (e.type) {
                        ServerApiGatewayErrorType.CONFLICT -> "update.conflict"
                        ServerApiGatewayErrorType.APPLICATION_NOT_FOUND -> "application.not.found"
                        ServerApiGatewayErrorType.PROPERTY_NOT_FOUND -> "property.not.found"
                        ServerApiGatewayErrorType.HOST_NOT_FOUND -> "host.not.found"
                    },
                    null
                )
            )
        }

        app.exception(ImportPropertiesException::class.java) { e, ctx ->
            ctx.status(400)
            ctx.json(
                ServerExceptionResponse(
                    when (e.type) {
                        ImportErrorType.ILLEGAL_FORMAT -> "illegal.format"
                    },
                    null
                )
            )
        }
    }
}

fun notFound(): Nothing = throw NotFoundResponse()

fun permissionDenied(): Nothing = throw PermissionDenied()

private data class ServerExceptionResponse(val code: String, val details: String?)
