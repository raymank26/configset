package com.configset.dashboard.pages

import arrow.core.Either
import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.TemplateRenderer
import com.configset.dashboard.util.RequestIdProducer
import com.configset.dashboard.util.formParamSafe
import com.configset.dashboard.util.htmxRedirect
import com.configset.dashboard.util.htmxShowAlert
import com.configset.dashboard.util.permissionDenied
import com.configset.dashboard.util.userInfo
import com.configset.sdk.auth.Admin
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post

class ApplicationsController(
    private val serverApiGateway: ServerApiGateway,
    private val templateRenderer: TemplateRenderer,
    private val requestIdProducer: RequestIdProducer,
) {

    fun bind() {
        get("applications") { ctx ->
            val applications = serverApiGateway.listApplications(ctx.userInfo())
            ctx.html(templateRenderer.render(ctx, "applications.html", mapOf("applications" to applications)))
        }
        get("applications/create") { ctx ->
            if (!ctx.userInfo().roles.contains(Admin)) {
                ctx.permissionDenied()
            }
            ctx.html(
                templateRenderer.render(
                    ctx, "update_application.html", mapOf(
                        "requestId" to requestIdProducer.nextRequestId()
                    )
                )
            )
        }
        post("applications/update") { ctx ->
            if (!ctx.userInfo().roles.contains(Admin)) {
                ctx.permissionDenied()
            }
            val applicationId = ctx.formParam("id")
                ?.ifBlank { null }
                ?.toLong()
            val requestId = ctx.formParamSafe("requestId")
            val applicationName = ctx.formParamSafe("applicationName")
            if (applicationId == null) {
                when (val result = serverApiGateway.createApplication(requestId, applicationName, ctx.userInfo())) {
                    is Either.Left -> ctx.htmxShowAlert(result.value.name)
                    is Either.Right -> ctx.htmxRedirect("/applications")
                }
            } else {
                TODO()
            }
        }
        delete("applications/delete") { ctx ->
            if (!ctx.userInfo().roles.contains(Admin)) {
                ctx.permissionDenied()
            }
            val appName = ctx.formParamSafe("applicationName")
            val requestId = requestIdProducer.nextRequestId()
            when (val result = serverApiGateway.deleteApplication(appName, requestId, ctx.userInfo())) {
                is Either.Left -> ctx.htmxShowAlert(result.value.name)
                is Either.Right -> ctx.htmxRedirect("/applications")
            }
        }
    }
}