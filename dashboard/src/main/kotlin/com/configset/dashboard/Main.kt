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
        val authenticationProvider = dependencyFactory.authenticationProvider(objectMapper);
        val authInterceptor = dependencyFactory.authInterceptor(authenticationProvider)
        val templateRenderer = dependencyFactory.templateRenderer()
        val configSetClient = dependencyFactory.configSetClient()
        val serverApiGateway = dependencyFactory.serverApiGateway(configSetClient)
        val listPropertiesService = dependencyFactory.listPropertyService(serverApiGateway)
        val requestIdProducer = dependencyFactory.requestIdProducer()
        val crudPropertyService = dependencyFactory.crudPropertyService(
            serverApiGateway,
            requestIdProducer
        )
        val propertiesController = dependencyFactory.propertiesController(
            templateRenderer,
            listPropertiesService,
            crudPropertyService,
            requestIdProducer
        )
        val applicationsController = dependencyFactory.applicationsController(
            serverApiGateway,
            templateRenderer,
            requestIdProducer
        )
//        val propertyImportService = dependencyFactory.propertyImportService(
//            serverApiGateway,
//            requestIdProducer
//        )
        val requestExtender = dependencyFactory.requestExtender(objectMapper)
        val javalinServer = dependencyFactory.javalinServer(
            authController,
            javalinExceptionMapper,
            listOf(authInterceptor, requestExtender),
            propertiesController,
            applicationsController
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