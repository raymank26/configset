package com.configset.dashboard.pages

import com.configset.dashboard.TemplateRenderer
import io.javalin.apibuilder.ApiBuilder.get

class PagesController(private val templateRenderer: TemplateRenderer) {

    fun bind() {
        get("") { ctx ->
            try {
                val res = templateRenderer.render("search.html")
                ctx.html(res)
            } catch (e: Exception) {
                println(e)
            }
        }
    }
}