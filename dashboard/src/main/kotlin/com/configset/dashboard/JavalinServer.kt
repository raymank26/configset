package com.configset.dashboard

import com.configset.dashboard.auth.AuthController
import com.configset.dashboard.auth.AuthInterceptor
import com.configset.dashboard.pages.PagesController
import com.configset.dashboard.util.JavalinExceptionMapper
import com.configset.sdk.extension.createLoggerStatic
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path

private val LOG = createLoggerStatic<JavalinServer>()

class JavalinServer(
    private val authController: AuthController,
    private val serverConfig: Config,
    private val javalinExceptionMapper: JavalinExceptionMapper,
    private val authInterceptor: AuthInterceptor,
    private val pagesController: PagesController,
) {

    private val app = Javalin.create { config ->
//        if (serverConfig.serveStatic) {
//            config.addStaticFiles("/dashboard/frontend", Location.EXTERNAL)
//            config.addSinglePageRoot("/", "/dashboard/frontend/index.html", Location.EXTERNAL)
//            LOG.info("Serve static configured")
//        }
    }

    fun start() {
        javalinExceptionMapper.bind(app)

        app.before(authInterceptor)
        app.routes {
            pagesController.bind()
            authController.bind()

            path("api") {
//                path("application") {
//                    applicationController.bind()
//                }
//                path("property") {
//                    propertyController.bind()
//                }
            }
        }
        app.start(serverConfig.dashboardPort)
        LOG.info("Server started")
    }

    fun stop() {
        app.stop()
    }
}