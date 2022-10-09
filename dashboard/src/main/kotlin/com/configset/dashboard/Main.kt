package com.configset.dashboard

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val config = Config(System.getenv())
        val app = createApp(DependencyFactory(config))
        Runtime.getRuntime().addShutdownHook(Thread {
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
        val applicationController = dependencyFactory.applicationController(serverApiGateway)
        val requestIdProducer = dependencyFactory.requestIdProducer()
        val propertyImportService = dependencyFactory.propertyImportService(
            serverApiGateway,
            requestIdProducer
        )
        val crudPropertyService = dependencyFactory.crudPropertyService(
            serverApiGateway,
            requestIdProducer
        )
        val propertyController = dependencyFactory.propertyController(
            crudPropertyService,
            listPropertiesService, propertyImportService
        )
        val javalinServer = dependencyFactory.javalinServer(
            authController,
            javalinExceptionMapper,
            authInterceptor,
            pagesController,
            applicationController,
            propertyController,
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