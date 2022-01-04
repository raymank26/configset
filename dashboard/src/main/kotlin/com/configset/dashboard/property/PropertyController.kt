package com.configset.dashboard.property

import com.configset.dashboard.PropertyCreateResult
import com.configset.dashboard.PropertyDeleteResult
import com.configset.dashboard.SearchPropertiesRequest
import com.configset.dashboard.util.BadRequest
import com.configset.dashboard.util.accessToken
import com.configset.dashboard.util.formParamSafe
import com.configset.dashboard.util.queryParamSafe
import com.configset.dashboard.util.requestId
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post

class PropertyController(
        private val crudPropertyService: CrudPropertyService,
        private val listPropertiesService: ListPropertiesService,
        private val propertyImportService: PropertyImportService
) {

    fun bind() {
        post("update") { ctx ->
            val appName = ctx.formParamSafe("applicationName")
            val hostName = ctx.formParamSafe("hostName")
            val propertyName = ctx.formParamSafe("propertyName")
            val propertyValue = ctx.formParamSafe("propertyValue")
            val version = ctx.formParam("version")?.toLong()
            val requestId = ctx.requestId()

            when (crudPropertyService.updateProperty(requestId,
                appName,
                hostName,
                propertyName,
                propertyValue,
                version, ctx.accessToken())) {
                PropertyCreateResult.OK -> Unit
                PropertyCreateResult.ApplicationNotFound -> throw BadRequest("application.not.found")
                PropertyCreateResult.UpdateConflict -> throw BadRequest("update.conflict")
            }
        }

        post("delete") { ctx ->
            val appName = ctx.formParamSafe("applicationName")
            val hostName = ctx.formParamSafe("hostName")
            val propertyName = ctx.formParamSafe("propertyName")
            val version = ctx.formParamSafe("version").toLong()
            val requestId = ctx.requestId()

            when (crudPropertyService.deleteProperty(requestId, appName, hostName, propertyName, version,
                ctx.accessToken())) {

                PropertyDeleteResult.OK -> Unit
                PropertyDeleteResult.DeleteConflict -> throw BadRequest("delete.conflict")
            }
        }

        post("import") { ctx ->
            val appName = ctx.formParamSafe("applicationName")
            val properties = ctx.formParamSafe("properties")
            val requestId = ctx.requestId()
            when (propertyImportService.import(requestId, appName, properties, ctx.accessToken())) {
                PropertiesImport.ApplicationNotFound -> throw BadRequest("application.not.found")
                PropertiesImport.OK -> Unit
                PropertiesImport.IllegalFormat -> throw BadRequest("illegal.format")
            }
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
                ctx.contentType("application/json").result("null")
            } else {
                ctx.json(property)
            }
        }
    }
}