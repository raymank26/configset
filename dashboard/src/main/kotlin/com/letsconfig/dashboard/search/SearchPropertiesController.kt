package com.letsconfig.dashboard.search

import com.letsconfig.dashboard.ServerApiGateway
import io.javalin.apibuilder.ApiBuilder.get

class SearchPropertiesController(
        private val serverApiGateway: ServerApiGateway
) {

    fun bind() {
        get("search") { ctx ->

        }
    }
}