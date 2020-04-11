package com.letsconfig.network.grpc

import com.letsconfig.extension.createLogger
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import java.io.PrintWriter

import java.io.StringWriter

class LoggingInterceptor : ServerInterceptor {
    private val log = createLogger()

    override fun <ReqT, RespT> interceptCall(call: ServerCall<ReqT, RespT>, headers: Metadata, next: ServerCallHandler<ReqT, RespT>): ServerCall.Listener<ReqT> {
        val wrapper = object : SimpleForwardingServerCall<ReqT, RespT>(call) {

            override fun close(status: Status, trailers: Metadata) {
                val newStatus = if (status.code === Status.Code.UNKNOWN && status.description == null && status.cause != null) {
                    val e = status.cause
                    log.info("Exception during request processing", e);
                    Status.INTERNAL
                            .withDescription(e!!.message)
                            .augmentDescription(stacktraceToString(e))
                } else {
                    status
                }
                super.close(newStatus, trailers)
            }
        }
        return next.startCall(wrapper, headers);
    }

    private fun stacktraceToString(e: Throwable): String? {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)
        return stringWriter.toString()
    }
}