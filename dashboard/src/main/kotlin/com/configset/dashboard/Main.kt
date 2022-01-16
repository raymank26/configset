package com.configset.dashboard

import com.configset.sdk.extension.createLoggerStatic
import org.koin.dsl.koinApplication

private val LOG = createLoggerStatic<Main>()

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val config = Config(System.getenv())
        val koinApp = koinApplication {
            modules(mainModule, remoteClientModule)
        }.properties(mapOf(CONFIG_KEY to config))
        Runtime.getRuntime().addShutdownHook(Thread {
            koinApp.close()
            LOG.info("Application has exited normally")
        })
        koinApp.koin.get<JavalinServer>().start()
    }
}