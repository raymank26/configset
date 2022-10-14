package com.configset.dashboard.pages

import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.TemplateRenderer
import com.configset.dashboard.util.userInfo
import io.javalin.apibuilder.ApiBuilder.get

class ApplicationsController(
    private val serverApiGateway: ServerApiGateway,
    private val templateRenderer: TemplateRenderer,
) {

    fun bind() {
        get("applications") { ctx ->
            val applications = serverApiGateway.listApplications(ctx.userInfo())
            ctx.html(templateRenderer.render(ctx, "applications.html", mapOf("applications" to applications)))
        }
    }
}