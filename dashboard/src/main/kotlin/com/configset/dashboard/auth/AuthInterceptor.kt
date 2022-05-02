package com.configset.dashboard.auth

import com.configset.dashboard.util.PermissionDenied
import io.javalin.http.Context
import io.javalin.http.Handler

class AuthInterceptor(
    private val excludePaths: List<String>,
) : Handler {

    override fun handle(ctx: Context) {
        for (excludePath in excludePaths) {
            if (ctx.path() == excludePath) {
                return
            }
        }
        if (ctx.req.getHeader("Authorization") == null) {
            throw PermissionDenied()
        }
    }
}