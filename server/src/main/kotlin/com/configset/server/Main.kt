package com.configset.server

import com.configset.sdk.extension.createLogger
import com.configset.server.network.grpc.GrpcConfigurationServer
import org.koin.dsl.koinApplication
import java.util.concurrent.Semaphore

/**
 * Date: 15.02.17.
 */
object Main {

    private val LOG = createLogger()

    @JvmStatic
    fun main(args: Array<String>) {
        val config = getConfig() ?: return

        val koinApp = koinApplication {
            modules(createAppModules(config))
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            koinApp.close()
            LOG.info("Application has exited normally")
        })
        koinApp.koin.get<GrpcConfigurationServer>().start()
        val semaphore = Semaphore(0)
        LOG.info("Server started")
        semaphore.acquire()
    }

    private fun getConfig(): AppConfiguration? {
        val config = AppConfiguration(System.getenv())
        return try {
            config.validate()
            config
        } catch (e: ConfigKeyRequired) {
            System.err.println("Env key = '${e.configKey}' is required")
            null
        }
    }
}
