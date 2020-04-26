package com.letsconfig.dashboard.application

import com.letsconfig.dashboard.ServerApiGateway
import io.javalin.apibuilder.ApiBuilder.get

class ApplicationController(
        private val serverApiGateway: ServerApiGateway
) {

    fun bind() {
        get("list") { ctx ->
            ctx.json(serverApiGateway.listApplications())
        }
    }
}