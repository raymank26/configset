package com.configset.dashboard.pages

import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.TemplateRenderer
import com.configset.dashboard.util.queryParamSafe
import com.configset.dashboard.util.userInfo
import io.javalin.apibuilder.ApiBuilder

class HostsController(
    private val serverApiGateway: ServerApiGateway,
    private val templateRenderer: TemplateRenderer
) {

    fun bind() {
        ApiBuilder.get("hosts/suggest") { ctx ->
            val hostName = ctx.queryParamSafe("hostName")
            val foundApplications = serverApiGateway.listHosts(ctx.userInfo())
                .filter { it.contains(hostName) }
            ctx.html(
                templateRenderer.render(
                    ctx, "autocomplete_items.jinja2",
                    mapOf(
                        "items" to foundApplications
                    )
                )
            )
        }
    }
}
