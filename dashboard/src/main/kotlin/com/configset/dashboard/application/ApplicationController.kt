package com.configset.dashboard.application

import com.configset.dashboard.CreateApplicationResult
import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.util.BadRequest
import com.configset.dashboard.util.accessToken
import com.configset.dashboard.util.formParamSafe
import com.configset.dashboard.util.requestId
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post

class ApplicationController(
    private val serverApiGateway: ServerApiGateway,
) {

    fun bind() {
        get("list") { ctx ->
            ctx.json(serverApiGateway.listApplications(ctx.accessToken()))
        }
        post("") { ctx ->
            val appName = ctx.formParamSafe("appName")
            when (serverApiGateway.createApplication(ctx.requestId(), appName, ctx.accessToken())) {
                CreateApplicationResult.OK -> Unit
                CreateApplicationResult.ApplicationAlreadyExists -> throw BadRequest("already.exists")
            }
        }
    }
}