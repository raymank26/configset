package com.configset.dashboard

import com.configset.sdk.client.ConfigSetClient
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.dsl.module

val remoteClientModule = module {
    single {
        val client = ConfigSetClient(config().hostname, config().port)
        registerCallback(object : ScopeCallback {
            override fun onScopeClose(scope: Scope) {
                client.stop()
            }
        })
        client
    }
}