package com.configset.server
import com.configset.sdk.extension.createLogger
import com.configset.server.db.memory.InMemoryConfigurationDao
import com.configset.server.db.postgres.PostgreSqlConfigurationDao
import com.configset.server.network.grpc.GrpcConfigurationServer
import com.configset.server.network.grpc.GrpcConfigurationService
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.koin.core.context.startKoin
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.dsl.module
import java.util.concurrent.Semaphore

/**
 * Date: 15.02.17.
 */
object Main {

    private val LOG = createLogger()

    @JvmStatic
    fun main(args: Array<String>) {
        val config = getConfig() ?: return

        val koinApp = startKoin {
            modules(createMainModule(config))
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
        val config = AppConfiguration()
        return try {
            config.validate()
            config
        } catch (e: ConfigKeyRequired) {
            System.err.println("Env key = '${e.configKey}' is required")
            null
        }
    }
}

fun createMainModule(config: AppConfiguration) = module {

    single {
        when (config.getDaoType()) {
            DaoType.IN_MEMORY -> InMemoryConfigurationDao()
            DaoType.POSTGRES -> {
                val dbi = Jdbi.create(config.getJdbcUrl())
                dbi.installPlugin(SqlObjectPlugin())
                dbi.installPlugin(PostgresPlugin())
                PostgreSqlConfigurationDao(dbi)
            }
        }
    }

    single {
        ConfigurationService(get(), get())
    }

    single {
        ConfigurationResolver()
    }

    single {
        val dispatcher = PropertiesWatchDispatcher(get(), get(), get(), config.getUpdateDelayMs())
        dispatcher.start()
        dispatcher
    }

    single<Scheduler> {
        ThreadScheduler()
    }

    single {
        val server = GrpcConfigurationServer(get(), config.grpcPort())
        onClose {
            server.stop()
        }
        server
    }

    single {
        GrpcConfigurationService(get())
    }
}

private fun Scope.onClose(f: () -> Unit) {
    this.registerCallback(object : ScopeCallback {
        override fun onScopeClose(scope: Scope) {
            f.invoke()
        }
    })
}
