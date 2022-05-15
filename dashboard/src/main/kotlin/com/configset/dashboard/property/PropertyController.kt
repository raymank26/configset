package com.configset.dashboard.property

import com.configset.dashboard.SearchPropertiesRequest
import com.configset.dashboard.util.accessToken
import com.configset.dashboard.util.formParamSafe
import com.configset.dashboard.util.queryParamSafe
import com.configset.dashboard.util.requestId
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post

class PropertyController(
    private val crudPropertyService: CrudPropertyService,
    private val listPropertiesService: ListPropertiesService,
    private val propertyImportService: PropertyImportService,
) {

    fun bind() {
        post("update") { ctx ->
            val appName = ctx.formParamSafe("applicationName")
            val hostName = ctx.formParamSafe("hostName")
            val propertyName = ctx.formParamSafe("propertyName")
            val propertyValue = ctx.formParamSafe("propertyValue")
            val version = ctx.formParam("version")?.toLong()
            val requestId = ctx.requestId()

            crudPropertyService.updateProperty(
                requestId,
                appName,
                hostName,
                propertyName,
                propertyValue,
                version,
                ctx.accessToken())
        }

        post("delete") { ctx ->
            val appName = ctx.formParamSafe("applicationName")
            val hostName = ctx.formParamSafe("hostName")
            val propertyName = ctx.formParamSafe("propertyName")
            val version = ctx.formParamSafe("version").toLong()
            val requestId = ctx.requestId()

            crudPropertyService.deleteProperty(
                requestId,
                appName,
                hostName,
                propertyName,
                version,
                ctx.accessToken())
        }

        post("import") { ctx ->
            val appName = ctx.formParamSafe("applicationName")
            val properties = ctx.formParamSafe("properties")
            val requestId = ctx.requestId()
            propertyImportService.import(requestId, appName, properties, ctx.accessToken())
        }

        get("list") { ctx ->
            val appName = ctx.queryParamSafe("applicationName")
            ctx.json(listPropertiesService.list(appName, ctx.accessToken()))
        }

        get("search") { ctx ->
            val appName = ctx.queryParam("applicationName")
            val hostName = ctx.queryParam("hostName")
            val propertyName = ctx.queryParam("propertyName")
            val propertyValue = ctx.queryParam("propertyValue")

            ctx.json(listPropertiesService.searchProperties(
                SearchPropertiesRequest(appName, hostName, propertyName, propertyValue),
                ctx.accessToken()))
        }

        get("get") { ctx ->
            val appName = ctx.queryParamSafe("applicationName")
            val hostName = ctx.queryParamSafe("hostName")
            val propertyName = ctx.queryParamSafe("propertyName")
            val property = listPropertiesService.getProperty(appName, hostName, propertyName, ctx.accessToken())
            if (property == null) {
                ctx.contentType("application/json").result("{}")
            } else {
                ctx.json(property)
            }
        }
    }
}