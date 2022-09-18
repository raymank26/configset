package com.configset.dashboard.pages

import com.configset.dashboard.SearchPropertiesRequest
import com.configset.dashboard.TemplateRenderer
import com.configset.dashboard.property.ListPropertiesService
import com.configset.dashboard.util.accessToken
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post

class PagesController(
    private val templateRenderer: TemplateRenderer,
    private val listPropertiesService: ListPropertiesService,
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
    }
}