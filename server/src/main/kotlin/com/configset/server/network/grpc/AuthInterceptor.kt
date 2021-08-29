package com.configset.server.network.grpc

import com.configset.server.auth.Anonymous
import com.configset.server.auth.Authenticator
import com.configset.server.auth.UserInfo
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor

val USER_INFO_KEY: Context.Key<UserInfo> = Context.key("userInfo")

class AuthInterceptor(private val authenticator: Authenticator) : ServerInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val userInfo = getIdentity(headers.get(Metadata.Key.of("Authentication", ASCII_STRING_MARSHALLER)))
        val context = Context.current().withValue(USER_INFO_KEY, userInfo)
        return Contexts.interceptCall(context, call, headers, next)
    }

    private fun getIdentity(accessToken: String?): UserInfo {
        if (accessToken == null) {
            return Anonymous
        }
        return authenticator.getUserInfo(accessToken)
    }
}

fun userInfo(): UserInfo = USER_INFO_KEY.get()