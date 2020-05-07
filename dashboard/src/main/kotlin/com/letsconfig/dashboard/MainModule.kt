package com.letsconfig.dashboard

import com.letsconfig.dashboard.application.ApplicationController
import com.letsconfig.dashboard.property.CrudPropertyService
import com.letsconfig.dashboard.property.ListPropertiesService
import com.letsconfig.dashboard.property.PropertyController
import com.letsconfig.dashboard.property.PropertyImportService
import com.letsconfig.dashboard.util.ExceptionMapper
import com.letsconfig.dashboard.util.RequestIdProducer
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.dsl.module

val mainModule = module {

    single {
        val server = JavalinServer(get(), getProperty("dashboard.port"), get(), get(), getProperty("serve.static"))

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
        ExceptionMapper()
    }

    single {
        val gateway = ServerApiGateway(getProperty("config_server.hostname"), getProperty("config_server.port"))
        gateway.start()

        this.registerCallback(object : ScopeCallback {
            override fun onScopeClose(scope: Scope) {
                gateway.stop()
            }
        })
        gateway
    }
}
