package com.letsconfig.dashboard

import com.letsconfig.sdk.extension.createLoggerStatic
import org.koin.core.context.startKoin

private val LOG = createLoggerStatic<Main>()

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val koinApp = startKoin {
            modules(mainModule)
        }.properties(mapOf(
                Pair("config_server.hostname", System.getenv().getOrDefault("config_server.hostname", "localhost")),
                Pair("config_server.port", System.getenv().getOrDefault("config_server.port", "8988").toInt()),
                Pair("config_server.timeout", System.getenv()["config_server.timeout"]?.toLong() ?: 2000),
                Pair("dashboard.port", System.getenv().getOrDefault("dashboard.port", "8188").toInt()),
                Pair("serve.static", System.getenv()["serve.static"]?.toBoolean() ?: false)
        ))
        Runtime.getRuntime().addShutdownHook(Thread {
            koinApp.close()
            LOG.info("Application has exited normally")
        })
        koinApp.koin.get<JavalinServer>().start()
    }
}