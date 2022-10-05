package com.configset.dashboard

import kotlin.concurrent.thread

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val config = Config(System.getenv())
        val app = createApp(DependencyFactory(config))
        Runtime.getRuntime().addShutdownHook(thread {
            app.stop()
        })
        app.start()
    }

    fun createApp(dependencyFactory: DependencyFactory): App {
        val objectMapper = dependencyFactory.objectMapper()
        val authController = dependencyFactory.authController(objectMapper)
        val javalinExceptionMapper = dependencyFactory.javalinExceptionMapper()
        val authInterceptor = dependencyFactory.authInterceptor()
        val templateRenderer = dependencyFactory.templateRenderer()
        val configSetClient = dependencyFactory.configSetClient()
        val serverApiGateway = dependencyFactory.serverApiGateway(configSetClient)
        val listPropertiesService = dependencyFactory.listPropertyService(serverApiGateway)
        val pagesController = dependencyFactory.pagesController(templateRenderer, listPropertiesService)
        val javalinServer = dependencyFactory.javalinServer(
            authController,
            javalinExceptionMapper,
            authInterceptor,
            pagesController
        )
        return object : App {
            override fun start() {
                javalinServer.start()
            }

            override fun stop() {
                javalinServer.stop()
                configSetClient.stop()
            }
        }
    }
}

interface App {
    fun start()
    fun stop()
}