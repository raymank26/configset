package com.configset.dashboard.pages

import arrow.core.Either
import com.configset.dashboard.SearchPropertiesRequest
import com.configset.dashboard.TablePropertyItem
import com.configset.dashboard.TemplateRenderer
import com.configset.dashboard.property.CrudPropertyService
import com.configset.dashboard.property.ListPropertiesService
import com.configset.dashboard.util.RequestIdProducer
import com.configset.dashboard.util.formParamSafe
import com.configset.dashboard.util.htmxRedirect
import com.configset.dashboard.util.htmxShowAlert
import com.configset.dashboard.util.queryParamSafe
import com.configset.dashboard.util.requestId
import com.configset.dashboard.util.urlEncode
import com.configset.dashboard.util.userInfo
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post

class PropertiesController(
    private val templateRenderer: TemplateRenderer,
    private val listPropertiesService: ListPropertiesService,
    private val crudPropertyService: CrudPropertyService,
    private val requestIdProducer: RequestIdProducer
) {

    fun bind() {
        get("") { ctx ->
            ctx.redirect("/properties")
        }
        get("properties") { ctx ->
            val applicationName = ctx.queryParam("applicationName") ?: ""
            val hostName = ctx.queryParam("hostName") ?: ""
            val propertyName = ctx.queryParam("propertyName") ?: ""
            val propertyValue = ctx.queryParam("propertyValue") ?: ""
            val (properties, showProperties) = if (applicationName == ""
                && hostName == ""
                && propertyName == ""
                && propertyValue == ""
            ) {

                emptyList<TablePropertyItem>() to false
            } else {
                listPropertiesService.searchProperties(
                    SearchPropertiesRequest(
                        applicationName = applicationName,
                        hostNameQuery = hostName,
                        propertyNameQuery = propertyName,
                        propertyValueQuery = propertyValue
                    ),
                    ctx.userInfo()
                ) to true
            }
            ctx.html(
                templateRenderer.render(
                    ctx, "properties.html", mapOf(
                        "properties" to properties,
                        "showProperties" to showProperties,
                        "applicationName" to applicationName,
                        "hostName" to hostName,
                        "propertyName" to propertyName,
                        "propertyValue" to propertyValue,
                    )
                )
            )
        }
        get("properties/create") { ctx ->
            ctx.html(
                templateRenderer.render(
                    ctx, "update_property.html", mapOf(
                        "requestId" to requestIdProducer.nextRequestId()
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
            )
            ctx.html(
                templateRenderer.render(
                    ctx, "update_property.html", mapOf(
                        "property" to property,
                        "requestId" to requestIdProducer.nextRequestId()
                    )
                )
            )
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
                    ctx.htmxRedirect(buildString {
                        append("/?applicationName=")
                        append(appName.urlEncode())
                    })
            }
        }

        post("properties/update") { ctx ->
            val requestId = ctx.requestId()
            val appName = ctx.formParamSafe("applicationName")
            val hostName = ctx.formParamSafe("hostName")
            val propertyName = ctx.formParamSafe("propertyName")
            val propertyValue = ctx.formParamSafe("propertyValue")
            val version = ctx.formParamSafe("version").toLongOrNull()

            val result = crudPropertyService.updateProperty(
                requestId = requestId,
                appName = appName,
                hostName = hostName,
                propertyName = propertyName,
                propertyValue = propertyValue,
                version = version,
                userInfo = ctx.userInfo()
            )
            when (result) {
                is Either.Left -> {
                    val property = listPropertiesService.getProperty(
                        appName = appName,
                        hostName = hostName,
                        propertyName = propertyName,
                        userInfo = ctx.userInfo()
                    )
                    ctx.html(
                        templateRenderer.render(
                            ctx, "update_property.html", mapOf(
                                "property" to property,
                                "error" to result.value
                            )
                        )
                    )
                }

                is Either.Right -> ctx.redirect(buildString {
                    append("/?applicationName=")
                    append(appName.urlEncode())
                    append("&propertyName=")
                    append(propertyName.urlEncode())
                    append("&hostName=")
                    append(hostName.urlEncode())
                })
            }
        }
    }
}