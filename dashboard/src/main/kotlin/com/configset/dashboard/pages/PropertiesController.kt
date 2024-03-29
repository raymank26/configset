package com.configset.dashboard.pages

import arrow.core.Either
import arrow.core.handleError
import arrow.core.right
import com.configset.dashboard.SearchPropertiesRequest
import com.configset.dashboard.ServerApiGatewayErrorType
import com.configset.dashboard.TablePropertyItem
import com.configset.dashboard.TemplateRenderer
import com.configset.dashboard.forms.Form
import com.configset.dashboard.forms.FormField
import com.configset.dashboard.forms.FormFieldValidator.Companion.NOT_BLANK
import com.configset.dashboard.forms.InvalidForm
import com.configset.dashboard.property.CrudPropertyService
import com.configset.dashboard.property.ListPropertiesService
import com.configset.dashboard.util.RequestIdProducer
import com.configset.dashboard.util.binding
import com.configset.dashboard.util.formParamSafe
import com.configset.dashboard.util.htmxRedirect
import com.configset.dashboard.util.htmxShowAlert
import com.configset.dashboard.util.notFound
import com.configset.dashboard.util.queryParamSafe
import com.configset.dashboard.util.requestId
import com.configset.dashboard.util.urlEncode
import com.configset.dashboard.util.userInfo
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.Handler

class PropertiesController(
    private val templateRenderer: TemplateRenderer,
    private val listPropertiesService: ListPropertiesService,
    private val crudPropertyService: CrudPropertyService,
    private val requestIdProducer: RequestIdProducer
) {

    private val propertySearchForm = Form(
        listOf(
            FormField(
                label = "Application name",
                required = false,
                inlineLabel = true,
                name = "applicationName",
            ),
            FormField(
                label = "Host name",
                required = false,
                inlineLabel = true,
                name = "hostName",
            ),
            FormField(
                label = "Property name",
                required = false,
                inlineLabel = true,
                name = "propertyName",
            ),
            FormField(
                label = "Property value",
                required = false,
                inlineLabel = true,
                name = "propertyValue"
            )
        )
    )

    private val propertyForm = Form(
        listOf(
            FormField(
                label = "Application name",
                required = true,
                name = "applicationName",
                validation = NOT_BLANK
            ),
            FormField(
                label = "Host name",
                required = true,
                name = "hostName",
                validation = NOT_BLANK
            ),
            FormField(
                label = "Property name",
                required = true,
                name = "propertyName",
                validation = NOT_BLANK
            ),
            FormField(
                label = "Property value",
                required = true,
                name = "propertyValue"
            ),
            FormField(
                label = "RequestId",
                required = true,
                name = "requestId"
            ),
            FormField(
                label = "Version",
                required = true,
                name = "propertyVersion"
            ),
        )
    )

    private val propertyFormUpdateReadOnlyFields = setOf("applicationName", "hostName", "propertyName")

    fun bind() {
        get("") { ctx ->
            ctx.redirect("/properties")
        }
        get("properties") { ctx ->
            val result = binding<InvalidForm, SearchPropertiesResult> {
                val (form) = propertySearchForm.performValidation(ctx.queryParamMap()).bind()

                val applicationName = form.getField("applicationName").value ?: ""
                val hostName = form.getField("hostName").value ?: ""
                val propertyName = form.getField("propertyName").value ?: ""
                val propertyValue = form.getField("propertyValue").value ?: ""

                if (applicationName == "" && hostName == "" && propertyName == "" && propertyValue == "") {
                    SearchPropertiesResult(emptyList(), false, form).right()
                } else {
                    val properties = listPropertiesService.searchProperties(
                        SearchPropertiesRequest(
                            applicationName = applicationName,
                            hostNameQuery = hostName,
                            propertyNameQuery = propertyName,
                            propertyValueQuery = propertyValue
                        ),
                        ctx.userInfo()
                    )
                    SearchPropertiesResult(properties, true, form).right()
                }
            }.handleError {
                SearchPropertiesResult(emptyList(), false, it.form)
            }.orNull()!!

            val templateName = if (ctx.header("HX-Request") == "true") {
                "properties_search_result_block.jinja2"
            } else {
                "properties.jinja2"
            }

            ctx.html(
                templateRenderer.render(
                    ctx, templateName,
                    mapOf(
                        "form" to result.form,
                        "showProperties" to result.showProperties,
                        "properties" to result.properties
                    )
                )
            )
        }
        get("properties/create") { ctx ->
            ctx.html(
                templateRenderer.render(
                    ctx, "update_property.jinja2",
                    mapOf(
                        "form" to propertyForm.withDefaultValues(
                            mapOf("requestId" to requestIdProducer.nextRequestId())
                        ),
                    )
                )
            )
        }
        get("properties/update") { ctx ->
            val property = listPropertiesService.getProperty(
                appName = ctx.queryParamSafe("applicationName"),
                hostName = ctx.queryParamSafe("hostName"),
                propertyName = ctx.queryParamSafe("propertyName"),
                userInfo = ctx.userInfo()
            ) ?: notFound()

            val form = propertyForm.withDefaultValues(
                mapOf(
                    "applicationName" to property.applicationName,
                    "hostName" to property.hostName,
                    "propertyName" to property.propertyName,
                    "propertyValue" to property.propertyValue,
                    "propertyVersion" to property.version.toString(),
                    "requestId" to requestIdProducer.nextRequestId()
                )
            ).withReadonlyFields(propertyFormUpdateReadOnlyFields)

            ctx.html(templateRenderer.render(ctx, "update_property.jinja2", mapOf("form" to form)))
        }

        delete("properties/delete") { ctx ->
            val appName = ctx.formParamSafe("applicationName")
            val propertyName = ctx.formParamSafe("propertyName")
            val hostName = ctx.formParamSafe("hostName")
            val result = crudPropertyService.deleteProperty(
                requestId = requestIdProducer.nextRequestId(),
                appName = appName,
                hostName = hostName,
                propertyName = propertyName,
                version = ctx.formParamSafe("version").toLong(),
                userInfo = ctx.userInfo()
            )
            when (result) {
                is Either.Left -> ctx.htmxShowAlert(result.value.name)
                is Either.Right ->
                    ctx.htmxRedirect(
                        buildString {
                            append("/?applicationName=")
                            append(appName.urlEncode())
                        }
                    )
            }
        }

        val updateHandler = Handler { ctx ->
            val result = binding<UpdateError, Form> {
                val validatedForm = propertyForm.performValidation(ctx.formParamMap())
                    .map { it.form }
                    .mapLeft { UpdateError.FormValidationError(it.form) }
                    .bind()

                val appName = validatedForm.getField("applicationName").value!!
                val hostName = validatedForm.getField("hostName").value!!
                val propertyName = validatedForm.getField("propertyName").value!!
                val propertyValue = validatedForm.getField("propertyValue").value!!
                val version: Long? = validatedForm.getField("propertyVersion").value?.toLongOrNull()

                crudPropertyService.updateProperty(
                    requestId = ctx.requestId(),
                    appName = appName,
                    hostName = hostName,
                    propertyName = propertyName,
                    propertyValue = propertyValue,
                    version = version,
                    userInfo = ctx.userInfo()
                )
                    .mapLeft { UpdateError.ServerApiError(validatedForm, it) }
                    .map { validatedForm }
            }.mapLeft {
                if (it is UpdateError.ServerApiError && it.error == ServerApiGatewayErrorType.APPLICATION_NOT_FOUND) {
                    UpdateError.FormValidationError(it.form.withFieldError("applicationName", "Application not found"))
                } else {
                    it
                }
            }
            val readonlyFields = if (ctx.path() == "/properties/create") {
                emptySet()
            } else {
                propertyFormUpdateReadOnlyFields
            }

            when (result) {
                is Either.Left -> when (val errorType = result.value) {
                    is UpdateError.FormValidationError ->
                        ctx.html(
                            templateRenderer.render(
                                ctx, "update_property.jinja2",
                                mapOf(
                                    "form" to errorType.form
                                        .withReadonlyFields(readonlyFields),
                                )
                            )
                        )

                    is UpdateError.ServerApiError ->
                        ctx.html(
                            templateRenderer.render(
                                ctx, "update_property.jinja2",
                                mapOf(
                                    "form" to errorType.form
                                        .withCommonError(errorType.error.name)
                                        .withReadonlyFields(readonlyFields),
                                )
                            )
                        )
                }

                is Either.Right -> ctx.redirect(
                    buildString {
                        append("/?applicationName=")
                        append(result.value.getField("applicationName").name.urlEncode())
                        append("&propertyName=")
                        append(result.value.getField("propertyName").name.urlEncode())
                        append("&hostName=")
                        append(result.value.getField("hostName").name.urlEncode())
                    }
                )
            }
        }

        post("properties/update", updateHandler)
        post("properties/create", updateHandler)
    }
}

sealed class UpdateError {
    data class FormValidationError(val form: Form) : UpdateError()
    data class ServerApiError(val form: Form, val error: ServerApiGatewayErrorType) : UpdateError()
}

data class SearchPropertiesResult(
    val properties: List<TablePropertyItem>,
    val showProperties: Boolean,
    val form: Form
)
