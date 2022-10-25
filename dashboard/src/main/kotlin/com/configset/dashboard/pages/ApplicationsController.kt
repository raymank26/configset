package com.configset.dashboard.pages

import arrow.core.Either
import arrow.core.computations.either
import com.configset.common.backend.auth.Admin
import com.configset.common.client.ApplicationId
import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.TemplateRenderer
import com.configset.dashboard.forms.Form
import com.configset.dashboard.forms.FormField
import com.configset.dashboard.forms.FormFieldValidator
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
import io.javalin.http.Handler

class ApplicationsController(
    private val serverApiGateway: ServerApiGateway,
    private val templateRenderer: TemplateRenderer,
    private val requestIdProducer: RequestIdProducer,
) {

    private val applicationForm = Form(
        listOf(
            FormField(
                label = "Application name",
                required = true,
                name = "applicationName",
                validation = FormFieldValidator.NOT_BLANK,
            ), FormField(
                label = "Application id",
                required = false,
                name = "applicationId",
                validation = FormFieldValidator.IS_LONG,
            ), FormField(
                label = "Request Id",
                required = true,
                name = "requestId",
                validation = FormFieldValidator.NOT_BLANK,
            )
        )
    )

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
                        "form" to applicationForm.withDefaultValues(
                            mapOf(
                                "requestId" to requestIdProducer.nextRequestId()
                            )
                        ),
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
                        "form" to applicationForm.withDefaultValues(
                            mapOf(
                                "applicationName" to application.name,
                                "applicationId" to application.id.id.toString(),
                                "requestId" to requestIdProducer.nextRequestId()
                            )
                        ),
                    )
                )
            )
        }
        val updateHandler = Handler { ctx ->
            if (!ctx.userInfo().roles.contains(Admin)) {
                permissionDenied()
            }
            val res = either.eager<UpdateError, Form> {
                val validForm = applicationForm.performValidation(ctx.formParamMap())
                    .map { it.form }
                    .mapLeft { UpdateError.FormValidationError(it.form) }
                    .bind()
                val applicationId = validForm.getField("applicationId").value
                val requestId = validForm.getField("requestId").value!!
                val applicationName = validForm.getField("applicationName").value!!
                if (applicationId.isNullOrBlank()) {
                    serverApiGateway.createApplication(requestId, applicationName, ctx.userInfo())
                        .mapLeft { UpdateError.ServerApiError(validForm, it) }
                        .map { validForm }
                        .bind()
                } else {
                    serverApiGateway.updateApplication(
                        id = ApplicationId(applicationId),
                        name = applicationName,
                        requestId = requestId,
                        userInfo = ctx.userInfo()
                    )
                        .mapLeft { UpdateError.ServerApiError(validForm, it) }
                        .map { validForm }
                        .bind()
                }
            }

            when (res) {
                is Either.Left -> when (val updateError = res.value) {
                    is UpdateError.FormValidationError ->
                        ctx.html(
                            templateRenderer.render(
                                ctx, "update_application.html", mapOf(
                                    "form" to updateError.form
                                )
                            )
                        )

                    is UpdateError.ServerApiError ->
                        ctx.html(
                            templateRenderer.render(
                                ctx, "update_application.html", mapOf(
                                    "form" to updateError.form
                                        .withCommonError(updateError.error.name)
                                )
                            )
                        )
                }

                is Either.Right -> ctx.redirect("/applications")
            }
        }
        post("applications/update", updateHandler)
        post("applications/create", updateHandler)

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