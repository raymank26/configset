package com.letsconfig.dashboard

import com.letsconfig.dashboard.application.ApplicationController
import com.letsconfig.dashboard.search.SearchPropertiesController
import com.letsconfig.dashboard.util.ExceptionMapper
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.dsl.module

val mainModule = module {

    single {
        val server = JavalinServer(get(), get(), getProperty("dashboard.port"), get())

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
        SearchPropertiesController(get())
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
