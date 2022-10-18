package com.configset.dashboard

import com.configset.common.backend.auth.AuthenticationProvider
import com.configset.common.backend.auth.RemoteAuthenticationProvider
import com.configset.common.client.ConfigSetClient
import com.configset.dashboard.auth.AuthController
import com.configset.dashboard.auth.AuthInterceptor
import com.configset.dashboard.pages.ApplicationsController
import com.configset.dashboard.pages.PropertiesController
import com.configset.dashboard.property.CrudPropertyService
import com.configset.dashboard.property.ListPropertiesService
import com.configset.dashboard.property.PropertyImportService
import com.configset.dashboard.util.JavalinExceptionMapper
import com.configset.dashboard.util.RequestIdProducer
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.http.Handler
import okhttp3.OkHttpClient

open class DependencyFactory(private val config: Config) {

    fun javalinServer(
        authController: AuthController,
        javalinExceptionMapper: JavalinExceptionMapper,
        interceptors: List<Handler>,
        propertiesController: PropertiesController,
        applicationsController: ApplicationsController,
    ): JavalinServer {
        return JavalinServer(
            authController,
            config,
            javalinExceptionMapper,
            interceptors,
            propertiesController,
            applicationsController,
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

    open fun authenticationProvider(objectMapper: ObjectMapper): AuthenticationProvider {
        val okHttpClient = OkHttpClient()
        return RemoteAuthenticationProvider(
            okHttpClient,
            config.authenticationConfig.realmUri,
            objectMapper,
            config.authenticationConfig.authClientId
        )
    }

    fun authInterceptor(authenticationProvider: AuthenticationProvider): AuthInterceptor {
        return AuthInterceptor(
            listOf("/api/config", "/auth/redirect"),
            config.authenticationConfig,
            authenticationProvider
        )
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

    fun propertiesController(
        templateRenderer: TemplateRenderer,
        listPropertiesService: ListPropertiesService,
        crudPropertyService: CrudPropertyService,
        requestIdProducer: RequestIdProducer
    ): PropertiesController {
        return PropertiesController(
            templateRenderer,
            listPropertiesService,
            crudPropertyService,
            requestIdProducer
        )
    }

    fun applicationsController(
        serverApiGateway: ServerApiGateway,
        templateRenderer: TemplateRenderer,
        requestIdProducer: RequestIdProducer
    ): ApplicationsController {
        return ApplicationsController(serverApiGateway, templateRenderer, requestIdProducer)
    }

    fun requestExtender(objectMapper: ObjectMapper): RequestExtender {
        return RequestExtender(objectMapper)
    }

    open fun configSetClient(): ConfigSetClient {
        return ConfigSetClient(config.hostname, config.port)
    }
}