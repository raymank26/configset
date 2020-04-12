package com.letsconfig
import com.letsconfig.db.memory.InMemoryConfigurationDao
import com.letsconfig.db.postgres.PostgreSqlConfigurationDao
import com.letsconfig.network.grpc.GrpcConfigurationServer
import com.letsconfig.network.grpc.GrpcConfigurationService
import org.jdbi.v3.core.Jdbi
import org.koin.core.context.startKoin
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.dsl.module
import java.util.concurrent.Semaphore

/**
 * Date: 15.02.17.
 */

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val config = getConfig() ?: return

        val koinApp = startKoin {
            modules(mainModule(config))
        }
        koinApp.koin.get<GrpcConfigurationServer>().start()
        val semaphore = Semaphore(0)
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

fun mainModule(config: AppConfiguration) = module {

    single {
        when (config.getDaoType()) {
            DaoType.IN_MEMORY -> InMemoryConfigurationDao()
            DaoType.POSTGRES -> {
                PostgreSqlConfigurationDao(Jdbi.create(config.getJdbcUrl()))
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
        PropertiesWatchDispatcher(get(), get(), get(), config.getUpdateDelayMs())
    }

    single<Scheduler> {
        ThreadScheduler()
    }

    single<GrpcConfigurationServer> {
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
