package com.configset.dashboard.pages

import arrow.core.Either
import com.configset.common.backend.auth.Admin
import com.configset.common.client.ApplicationId
import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.TemplateRenderer
import com.configset.dashboard.util.RequestIdProducer
import com.configset.dashboard.util.formParamSafe
import com.configset.dashboard.util.htmxRedirect
import com.configset.dashboard.util.htmxShowAlert
import com.configset.dashboard.util.notFound
import com.configset.dashboard.util.permissionDenied
import com.configset.dashboard.util.queryParamSafe
import com.configset.dashboard.util.userInfo
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
                permissionDenied()
            }
            ctx.html(
                templateRenderer.render(
                    ctx, "update_application.html", mapOf(
                        "requestId" to requestIdProducer.nextRequestId()
                    )
                )
            )
        }
        get("applications/update") { ctx ->
            if (!ctx.userInfo().roles.contains(Admin)) {
                permissionDenied()
            }
            val appName = ctx.queryParamSafe("applicationName")
            val application = serverApiGateway.listApplications(ctx.userInfo())
                .find { it.name == appName }
                ?: notFound()
            ctx.html(
                templateRenderer.render(
                    ctx, "update_application.html", mapOf(
                        "application" to application,
                        "requestId" to requestIdProducer.nextRequestId()
                    )
                )
            )
        }
        post("applications/update") { ctx ->
            if (!ctx.userInfo().roles.contains(Admin)) {
                permissionDenied()
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
                when (val result = serverApiGateway.updateApplication(
                    ApplicationId(applicationId),
                    applicationName,
                    requestId,
                    ctx.userInfo()
                )) {
                    is Either.Left -> ctx.htmxShowAlert(result.value.name)
                    is Either.Right -> ctx.htmxRedirect("/applications")
                }
            }
        }
        delete("applications/delete") { ctx ->
            if (!ctx.userInfo().roles.contains(Admin)) {
                permissionDenied()
            }
            val appName = ctx.formParamSafe("applicationName")
            val requestId = requestIdProducer.nextRequestId()
            when (val result = serverApiGateway.deleteApplication(appName, requestId, ctx.userInfo())) {
                is Either.Left -> ctx.htmxShowAlert(result.value.name)
                is Either.Right -> ctx.htmxRedirect("/applications")
            }
        }
        get("applications/suggest") { ctx ->
            val applicationName = ctx.queryParamSafe("applicationName")
            val foundApplications = serverApiGateway.listApplications(ctx.userInfo())
                .filter { it.name.contains(applicationName) }
                .map { it.name }
            ctx.html(
                templateRenderer.render(
                    ctx, "autocomplete_items.html", mapOf(
                        "items" to foundApplications
                    )
                )
            )
        }
    }
}