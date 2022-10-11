package com.configset.dashboard.pages

import arrow.core.Either
import com.configset.dashboard.SearchPropertiesRequest
import com.configset.dashboard.TemplateRenderer
import com.configset.dashboard.property.CrudPropertyService
import com.configset.dashboard.property.ListPropertiesService
import com.configset.dashboard.util.RequestIdProducer
import com.configset.dashboard.util.accessToken
import com.configset.dashboard.util.escapeHtml
import com.configset.dashboard.util.formParamSafe
import com.configset.dashboard.util.queryParamSafe
import com.configset.dashboard.util.requestId
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post

class PagesController(
    private val templateRenderer: TemplateRenderer,
    private val listPropertiesService: ListPropertiesService,
    private val crudPropertyService: CrudPropertyService,
    private val requestIdProducer: RequestIdProducer
) {

    fun bind() {
        get("") { ctx ->
            ctx.html(templateRenderer.render("search.html"))
        }
        path("api") {
            path("property") {
                post("search") { ctx ->
                    val properties = listPropertiesService.searchProperties(
                        SearchPropertiesRequest(
                            applicationName = ctx.formParam("application-name"),
                            hostNameQuery = ctx.formParam("hostname"),
                            propertyNameQuery = ctx.formParam("property-name"),
                            propertyValueQuery = ctx.formParam("property-value")
                        ),
                        ctx.accessToken()
                    )
                    ctx.html(templateRenderer.render("properties_block.html", mapOf("properties" to properties)))
                }
            }
        }
        get("update") { ctx ->
            val property = listPropertiesService.getProperty(
                appName = ctx.queryParamSafe("applicationName"),
                hostName = ctx.queryParamSafe("hostName"),
                propertyName = ctx.queryParamSafe("propertyName"),
                accessToken = ctx.accessToken()
            )
            ctx.html(
                templateRenderer.render(
                    "update_property.html", mapOf(
                        "property" to property,
                        "requestId" to requestIdProducer.nextRequestId()
                    )
                )
            )
        }

        post("update") { ctx ->
            val requestId = ctx.requestId()
            val appName = ctx.formParamSafe("applicationName")
            val hostName = ctx.formParamSafe("hostName")
            val propertyName = ctx.formParamSafe("propertyName")

            val result = crudPropertyService.updateProperty(
                requestId = requestId,
                appName = appName,
                hostName = hostName,
                propertyName = propertyName,
                propertyValue = ctx.formParamSafe("propertyValue"),
                version = ctx.formParamSafe("version").toLongOrNull(),
                ctx.accessToken()
            )
            when (result) {
                is Either.Left -> {
                    val property = listPropertiesService.getProperty(
                        appName = appName,
                        hostName = hostName,
                        propertyName = propertyName,
                        accessToken = ctx.accessToken()
                    )
                    ctx.html(
                        templateRenderer.render(
                            "update_property.html", mapOf(
                                "property" to property,
                                "error" to result.value
                            )
                        )
                    )
                }

                is Either.Right -> ctx.redirect(buildString {
                    append("/?applicationName=")
                    append(appName.escapeHtml())
                    append("propertyName=")
                    append(propertyName.escapeHtml())
                    append("hostName=")
                    append(hostName.escapeHtml())
                })
            }
        }
    }
}