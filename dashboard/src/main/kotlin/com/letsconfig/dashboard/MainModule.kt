package com.letsconfig.dashboard

import org.koin.dsl.module

val mainModule = module {

    single {
        JavalinServer()
    }
}
