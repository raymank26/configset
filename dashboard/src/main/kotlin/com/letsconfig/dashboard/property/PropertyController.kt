package com.letsconfig.dashboard.property

import com.letsconfig.dashboard.PropertyCreateResult
import com.letsconfig.dashboard.util.BadRequest
import com.letsconfig.dashboard.util.formParamSafe
import com.letsconfig.dashboard.util.requestId
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post

class PropertyController(
        private val createPropertyService: CreatePropertyService,
        private val listPropertiesService: ListPropertiesService
) {

    fun bind() {
        post("update") { ctx ->
            val appName = ctx.formParamSafe("applicationName")
            val hostName = ctx.formParamSafe("hostName")
            val propertyName = ctx.formParamSafe("propertyName")
            val propertyValue = ctx.formParamSafe("propertyValue")
            val version = ctx.formParam("version")?.toLong()

            when (createPropertyService.createProperty(ctx.requestId(), appName, hostName, propertyName, propertyValue, version)) {
                PropertyCreateResult.OK -> Unit
                PropertyCreateResult.HostNotFound -> throw BadRequest("host.not.found")
                PropertyCreateResult.ApplicationNotFound -> throw BadRequest("application.not.found")
                PropertyCreateResult.UpdateConflict -> throw BadRequest("update.conflict")
            }
        }
        get("list") { ctx ->
            val appName = ctx.formParam("applicationName")
            val hostName = ctx.formParam("hostName")
            val propertyName = ctx.formParam("propertyName")
            val propertyValue = ctx.formParam("propertyValue")

//            listPropertiesService.list(appName, hostName, propertyName, propertyValue)
        }
    }
}