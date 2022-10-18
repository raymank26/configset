package com.configset.server.network.grpc

import com.configset.common.client.extension.createLogger
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusRuntimeException

class LoggingInterceptor : ServerInterceptor {
    private val log = createLogger()

    override fun <ReqT, RespT> interceptCall(call: ServerCall<ReqT, RespT>, headers: Metadata, next: ServerCallHandler<ReqT, RespT>): ServerCall.Listener<ReqT> {
        val listener: ServerCall.Listener<ReqT> = next.startCall(call, headers)

        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            override fun onHalfClose() {
                try {
                    super.onHalfClose()
                } catch (e: Exception) {
                    log.warn("Exception occurred for method = ${call.methodDescriptor.fullMethodName}", e)
                    if (e is StatusRuntimeException) {
                        call.close(e.status, Metadata())
                    } else {
                        call.close(Status.INTERNAL
                            .withCause(e)
                            .withDescription(e.toString()), Metadata())
                    }
                }
            }
        }
    }
}