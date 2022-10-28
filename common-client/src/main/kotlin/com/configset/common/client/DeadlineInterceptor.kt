package com.configset.common.client

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.Context
import io.grpc.ForwardingClientCall
import io.grpc.MethodDescriptor
import java.util.concurrent.TimeUnit

class DeadlineInterceptor(private val deadlineMs: Long) : ClientInterceptor {
    override fun <ReqT : Any, RespT : Any> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        val co = if (callOptions.deadline == null && Context.current().deadline == null) {
            callOptions.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
        } else {
            callOptions
        }
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, co)) {}
    }
}
