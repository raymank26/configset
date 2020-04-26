package com.letsconfig.dashboard

import com.letsconfig.dashboard.application.ApplicationController
import com.letsconfig.dashboard.property.PropertyController
import com.letsconfig.dashboard.util.ExceptionMapper
import com.letsconfig.sdk.extension.createLoggerStatic
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path

private val LOG = createLoggerStatic<JavalinServer>()

class JavalinServer(
        private val applicationController: ApplicationController,
        private val dashboardPort: Int,
        private val exceptionMapper: ExceptionMapper,
        private val propertyController: PropertyController
) {

    private val app = Javalin.create()

    fun start() {

        exceptionMapper.bind(app)
        app.get("/api/hello") { ctx -> ctx.result("Hello World") }

        app.routes {
            path("api") {
                path("application") {
                    applicationController.bind()
                }
                path("property") {
                    propertyController.bind()
                }
            }
        }
        app.start(dashboardPort)
        LOG.info("Server started")
    }

    fun stop() {
        app.stop()
    }
}