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
import com.fasterxml.jackson.databind.ObjectMapper
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.dsl.module

const val CONFIG_KEY = "config"

val mainModule = module {

    single {
        val server = JavalinServer(get(), config(), get(), get(), get())

        this.registerCallback(object : ScopeCallback {
            override fun onScopeClose(scope: Scope) {
                server.stop()
            }
        })
        server
    }

    single {
        ApplicationController(get())
    }

    single {
        PropertyController(get(), get(), get())
    }

    single {
        PropertyImportService(get(), get())
    }

    single {
        ListPropertiesService(get())
    }

    single {
        CrudPropertyService(get(), get())
    }

    single {
        RequestIdProducer()
    }

    single {
        JavalinExceptionMapper()
    }

    single {
        val gateway = ServerApiGateway(get())
        this.registerCallback(object : ScopeCallback {
            override fun onScopeClose(scope: Scope) {
                gateway.stop()
            }
        })
        gateway
    }

    single {
        AuthInterceptor(
            listOf("/api/config", "/auth/redirect"),
            config().authUri,
            config().authRedirectUri,
            config().authClientId
        )
    }

    single {
        AuthController(
            config().authSecretKey, config().authClientId, config().requestTokenUri,
            config().authRedirectUri, get()
        )
    }

    single {
        ObjectMapper()
    }

    single {
        TemplateRenderer(config().templatesFilePath)
    }

    single {
        PagesController(get(), get())
    }
}

fun Scope.config(): Config = getProperty(CONFIG_KEY)
