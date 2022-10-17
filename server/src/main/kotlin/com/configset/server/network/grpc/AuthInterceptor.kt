package com.configset.server.network.grpc

import com.configset.common.backend.auth.AuthenticationProvider
import com.configset.common.backend.auth.Role
import com.configset.common.backend.auth.UserInfo
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.time.Instant

val USER_INFO_KEY: Context.Key<UserInfo> = Context.key("userInfo")

class AuthInterceptor(private val authenticationProvider: AuthenticationProvider) : ServerInterceptor {
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
        return authenticationProvider.authenticate(accessToken) ?: throw StatusRuntimeException(Status.UNAUTHENTICATED)
    }
}

fun userInfo(): UserInfo = USER_INFO_KEY.get()

object Anonymous : UserInfo {
    override val accessToken: String = "some.token"
    override val userName: String = "anonymous"
    override val roles: Set<Role> = setOf()

    override fun accessTokenExpired(instant: Instant): Boolean = false
}