package com.configset.server

import com.configset.server.auth.Admin
import com.configset.server.auth.Authenticator
import com.configset.server.auth.LoggedIn
import com.configset.server.auth.OAuth2Authenticator
import com.configset.server.auth.StubAuthenticator
import com.configset.server.auth.UserRoleService
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.DbHandleFactory
import com.configset.server.db.RequestIdDao
import com.configset.server.db.memory.InMemoryConfigurationDao
import com.configset.server.db.memory.InMemoryDbHandleFactory
import com.configset.server.db.memory.InMemoryRequestIdDao
import com.configset.server.db.postgres.DbMigrator
import com.configset.server.db.postgres.PostgreSqlConfigurationDao
import com.configset.server.db.postgres.PostgresDbHandleFactory
import com.configset.server.db.postgres.RequestIdSqlDao
import com.configset.server.network.grpc.GrpcConfigurationServer
import com.configset.server.network.grpc.GrpcConfigurationService
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.dsl.module
import java.net.http.HttpClient

fun createAppModules(config: AppConfiguration): List<Module> {
    val dbModule = createDbModule(config)
    val authModule = createAuthModule(config)

    return module {
        single {
            ConfigurationService(get(), get(), get(), get())
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
            val server = GrpcConfigurationServer(get(), get(), config.grpcPort())
            onClose {
                server.stop()
            }
            server
        }

        single {
            GrpcConfigurationService(get(), get())
        }

        single {
            UserRoleService()
        }
    }.plus(listOf(dbModule, authModule))
}


private fun createDbModule(config: AppConfiguration): Module {
    return when (config.getDaoType()) {
        DaoType.IN_MEMORY -> module {
            single {
                InMemoryDbHandleFactory
            }
            single<ConfigurationDao> {
                InMemoryConfigurationDao()
            }
            single<RequestIdDao> {
                InMemoryRequestIdDao()
            }
            single<DbHandleFactory> {
                InMemoryDbHandleFactory
            }
        }
        DaoType.POSTGRES -> module {
            single {
                val dbi = Jdbi.create(config.getJdbcUrl())
                dbi.installPlugin(SqlObjectPlugin())
                dbi.installPlugin(PostgresPlugin())
                DbMigrator(dbi).migrate()
                dbi
            }
            single<ConfigurationDao> {
                PostgreSqlConfigurationDao(get())
            }
            single<RequestIdDao> {
                RequestIdSqlDao()
            }
            single<DbHandleFactory> {
                PostgresDbHandleFactory(get())
            }
        }
    }
}

private fun createAuthModule(config: AppConfiguration): Module {
    return when (val c = config.getAuthenticatorConfig()) {
        is AuthConfiguration -> module {
            single<Authenticator> {
                OAuth2Authenticator(
                    baseUrl = c.baseUrl,
                    timeoutMs = 2000,
                    httpClient = HttpClient.newHttpClient()
                )
            }
        }
        is StubAuthenticatorConfig -> module {
            single<Authenticator> {
                StubAuthenticator(mapOf(c.adminAccessToken to LoggedIn("admin", setOf(Admin.key))))
            }
        }
    }
}

private fun Scope.onClose(f: () -> Unit) {
    this.registerCallback(object : ScopeCallback {
        override fun onScopeClose(scope: Scope) {
            f.invoke()
        }
    })
}
