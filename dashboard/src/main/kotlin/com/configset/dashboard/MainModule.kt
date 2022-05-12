package com.configset.dashboard

import com.configset.dashboard.application.ApplicationController
import com.configset.dashboard.auth.AuthInterceptor
import com.configset.dashboard.property.CrudPropertyService
import com.configset.dashboard.property.ListPropertiesService
import com.configset.dashboard.property.PropertyController
import com.configset.dashboard.property.PropertyImportService
import com.configset.dashboard.util.ExceptionMapper
import com.configset.dashboard.util.ExceptionMappingService
import com.configset.dashboard.util.RequestIdProducer
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
        ApplicationController(get(), get())
    }

    single {
        PropertyController(get(), get(), get(), get())
    }

    single {
        ExceptionMappingService()
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
        ExceptionMapper()
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
        AuthInterceptor(listOf("/api/config"))
    }
}

fun Scope.config(): Config = getProperty(CONFIG_KEY)
