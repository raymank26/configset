package com.letsconfig.dashboard

import com.letsconfig.dashboard.application.ApplicationController
import com.letsconfig.dashboard.search.SearchPropertiesController
import com.letsconfig.sdk.extension.createLoggerStatic
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path

private val LOG = createLoggerStatic<JavalinServer>()

class JavalinServer(
        private val searchPropertiesController: SearchPropertiesController,
        private val applicationController: ApplicationController,
        private val dashboardPort: Int
) {

    private val app = Javalin.create()

    fun start() {

        app.get("/api/hello") { ctx -> ctx.result("Hello World") }

        app.routes {
            path("api") {
                path("application") {
                    applicationController.bind()
                }
                path("property") {
                    searchPropertiesController.bind()
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