package com.configset.server.common

import com.configset.server.db.postgres.DbMigrator
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.junit.rules.ExternalResource
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer

private val LOG = LoggerFactory.getLogger(PostgresqlTestRule::class.java)

/**
 * @author anton.ermak
 * Date: 2019-08-27.
 */
class PostgresqlTestRule : ExternalResource() {

    private lateinit var container: KPostgreSQLContainer
    private lateinit var dbi: Jdbi

    override fun before() {
        container = KPostgreSQLContainer()
        container.start()
        dbi = getDataSource(container)
        dbi.installPlugin(SqlObjectPlugin())
        dbi.installPlugin(PostgresPlugin())
        DbMigrator(dbi).migrate()
        Runtime.getRuntime().addShutdownHook(Thread {
            teardown()
        })
    }

    override fun after() {
        getDBI().useHandle<Exception> {
            it.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public")
        }
        LOG.info("Database is recreated")
    }

    private fun getDataSource(container: KPostgreSQLContainer): Jdbi {
        return Jdbi.create(container.jdbcUrl, container.username, container.password)
    }

    fun getDBI(): Jdbi {
        return dbi
    }

    private fun teardown() {
        container.close()
        LOG.info("Docker container closed")
    }
}

class KPostgreSQLContainer : PostgreSQLContainer<KPostgreSQLContainer>("postgres:13")
