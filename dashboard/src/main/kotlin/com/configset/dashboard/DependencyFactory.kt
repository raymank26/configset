package com.configset.dashboard

import com.configset.dashboard.application.ApplicationController
import com.configset.dashboard.auth.AuthController
import com.configset.dashboard.auth.AuthInterceptor
import com.configset.dashboard.pages.PagesController
import com.configset.dashboard.property.CrudPropertyService
import com.configset.dashboard.property.ListPropertiesService
import com.configset.dashboard.property.PropertyController
import com.configset.dashboard.property.PropertyImportService
import com.configset.dashboard.util.JavalinExceptionMapper
import com.configset.dashboard.util.RequestIdProducer
import com.configset.sdk.client.ConfigSetClient
import com.fasterxml.jackson.databind.ObjectMapper

open class DependencyFactory(private val config: Config) {

    fun javalinServer(
        authController: AuthController,
        javalinExceptionMapper: JavalinExceptionMapper,
        authInterceptor: AuthInterceptor,
        pagesController: PagesController,
        applicationController: ApplicationController,
        propertyController: PropertyController,
    ): JavalinServer {
        return JavalinServer(
            authController,
            config,
            javalinExceptionMapper,
            authInterceptor,
            pagesController,
            applicationController,
            propertyController
        )
    }

    fun propertyImportService(
        serverApiGateway: ServerApiGateway,
        requestIdProducer: RequestIdProducer,
    ): PropertyImportService {
        return PropertyImportService(serverApiGateway, requestIdProducer)
    }

    fun listPropertyService(serverApiGateway: ServerApiGateway): ListPropertiesService {
        return ListPropertiesService(serverApiGateway)
    }

    fun crudPropertyService(
        serverApiGateway: ServerApiGateway,
        requestIdProducer: RequestIdProducer,
    ): CrudPropertyService {
        return CrudPropertyService(serverApiGateway, requestIdProducer)
    }

    fun requestIdProducer(): RequestIdProducer {
        return RequestIdProducer()
    }

    fun javalinExceptionMapper(): JavalinExceptionMapper {
        return JavalinExceptionMapper()
    }

    fun serverApiGateway(configSetClient: ConfigSetClient): ServerApiGateway {
        return ServerApiGateway(configSetClient)
    }

    fun authInterceptor(): AuthInterceptor {
        return AuthInterceptor(listOf("/api/config", "/auth/redirect"), config.authenticationConfig)
    }

    fun authController(objectMapper: ObjectMapper): AuthController {
        return AuthController(config.authenticationConfig, objectMapper)
    }

    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
    }

    fun templateRenderer(): TemplateRenderer {
        return TemplateRenderer(config.templatesFilePath)
    }

    fun pagesController(
        templateRenderer: TemplateRenderer,
        listPropertiesService: ListPropertiesService,
    ): PagesController {
        return PagesController(templateRenderer, listPropertiesService)
    }

    fun applicationController(serverApiGateway: ServerApiGateway): ApplicationController {
        return ApplicationController(serverApiGateway)
    }

    fun propertyController(
        crudPropertyService: CrudPropertyService,
        listPropertiesService: ListPropertiesService,
        propertyImportService: PropertyImportService,
    ): PropertyController {
        return PropertyController(crudPropertyService, listPropertiesService, propertyImportService)
    }

    open fun configSetClient(): ConfigSetClient {
        return ConfigSetClient(config.hostname, config.port)
    }
}