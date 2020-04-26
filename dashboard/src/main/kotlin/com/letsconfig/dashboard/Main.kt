package com.letsconfig.dashboard

import com.letsconfig.sdk.extension.createLoggerStatic
import org.koin.core.context.startKoin

private val LOG = createLoggerStatic<Main>()

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val koinApp = startKoin {
            modules(mainModule)
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            koinApp.close()
            LOG.info("Application has exited normally")
        })
        koinApp.koin.get<JavalinServer>().start()
    }
}