package com.configset.dashboard

import com.configset.dashboard.auth.AuthController
import com.configset.dashboard.pages.ApplicationsController
import com.configset.dashboard.pages.PropertiesController
import com.configset.dashboard.util.JavalinExceptionMapper
import com.configset.sdk.extension.createLoggerStatic
import io.javalin.Javalin
import io.javalin.http.Handler
import io.javalin.http.staticfiles.Location

private val LOG = createLoggerStatic<JavalinServer>()

class JavalinServer(
    private val authController: AuthController,
    private val serverConfig: Config,
    private val javalinExceptionMapper: JavalinExceptionMapper,
    private val interceptors: List<Handler>,
    private val propertiesController: PropertiesController,
    private val applicationsController: ApplicationsController,
) {

    private val app = Javalin.create { config ->
        config.addStaticFiles {
            it.hostedPath = "/js"
            it.directory = serverConfig.jsFilePath ?: "/js"
            it.location = serverConfig.jsFilePath?.let { Location.EXTERNAL } ?: Location.CLASSPATH
        }
        LOG.info("Static files configured")
    }

    fun start() {
        javalinExceptionMapper.bind(app)

        interceptors.forEach {
            app.before(it)
        }
        app.routes {
            propertiesController.bind()
            applicationsController.bind()
            authController.bind()
        }
        app.start(serverConfig.dashboardPort)
        LOG.info("Server started")
    }

    fun stop() {
        app.stop()
    }
}