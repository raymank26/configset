package com.letsconfig.dashboard.property

import com.letsconfig.dashboard.PropertyCreateResult
import com.letsconfig.dashboard.PropertyDeleteResult
import com.letsconfig.dashboard.SearchPropertiesRequest
import com.letsconfig.dashboard.util.BadRequest
import com.letsconfig.dashboard.util.formParamSafe
import com.letsconfig.dashboard.util.queryParamSafe
import com.letsconfig.dashboard.util.requestId
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

            when (crudPropertyService.updateProperty(requestId, appName, hostName, propertyName, propertyValue, version)) {
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

            when (crudPropertyService.deleteProperty(requestId, appName, hostName, propertyName, version)) {
                PropertyDeleteResult.OK -> Unit
                PropertyDeleteResult.DeleteConflict -> throw BadRequest("delete.conflict")
            }
        }

        post("import") { ctx ->
            val appName = ctx.formParamSafe("applicationName")
            val properties = ctx.formParamSafe("properties")
            val requestId = ctx.requestId()
            when (propertyImportService.import(requestId, appName, properties)) {
                PropertiesImport.ApplicationNotFound -> throw BadRequest("application.not.found")
                PropertiesImport.OK -> Unit
                PropertiesImport.IllegalFormat -> throw BadRequest("illegal.format")
            }
        }

        get("list") { ctx ->
            val appName = ctx.queryParamSafe("applicationName")
            ctx.json(listPropertiesService.list(appName))
        }

        get("search") { ctx ->
            val appName = ctx.queryParam("applicationName")
            val hostName = ctx.queryParam("hostName")
            val propertyName = ctx.queryParam("propertyName")
            val propertyValue = ctx.queryParam("propertyValue")

            ctx.json(listPropertiesService.searchProperties(SearchPropertiesRequest(appName, hostName, propertyName, propertyValue)))
        }
    }
}