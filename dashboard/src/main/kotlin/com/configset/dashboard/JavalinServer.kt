package com.configset.dashboard

import com.configset.dashboard.application.ApplicationController
import com.configset.dashboard.auth.AuthInterceptor
import com.configset.dashboard.pages.PagesController
import com.configset.dashboard.property.PropertyController
import com.configset.dashboard.util.ClientConfig
import com.configset.dashboard.util.JavalinExceptionMapper
import com.configset.sdk.extension.createLoggerStatic
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path

private val LOG = createLoggerStatic<JavalinServer>()

class JavalinServer(
    private val applicationController: ApplicationController,
    private val serverConfig: Config,
    private val javalinExceptionMapper: JavalinExceptionMapper,
    private val propertyController: PropertyController,
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

//        app.before(authInterceptor)
        app.routes {
            pagesController.bind()
            path("api") {
//                path("application") {
//                    applicationController.bind()
//                }
//                path("property") {
//                    propertyController.bind()
//                }
                get("config") {
                    it.json(
                        ClientConfig(
                            keycloackUrl = serverConfig.keycloackUrl,
                            keycloackRealm = serverConfig.keycloackRealm,
                            keycloackClientId = serverConfig.keycloackClientId
                        )
                    )
                }
            }
        }
        app.start(serverConfig.dashboardPort)
        LOG.info("Server started")
    }

    fun stop() {
        app.stop()
    }
}