package com.letsconfig.dashboard.application

import com.letsconfig.dashboard.CreateApplicationResult
import com.letsconfig.dashboard.ServerApiGateway
import com.letsconfig.dashboard.util.BadRequest
import com.letsconfig.dashboard.util.formParamSafe
import com.letsconfig.dashboard.util.requestId
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post

class ApplicationController(
        private val serverApiGateway: ServerApiGateway
) {

    fun bind() {
        get("list") { ctx ->
            ctx.json(serverApiGateway.listApplications())
        }
        post("") { ctx ->
            val appName = ctx.formParamSafe("appName")
            when (serverApiGateway.createApplication(ctx.requestId(), appName)) {
                CreateApplicationResult.OK -> Unit
                CreateApplicationResult.ApplicationAlreadyExists -> throw BadRequest("already.exists")
            }
        }
    }
}