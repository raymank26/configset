package com.letsconfig.server.common

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin

import org.junit.rules.ExternalResource
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

private val LOG = LoggerFactory.getLogger(PostgresqlTestRule::class.java)

/**
 * @author anton.ermak
 * Date: 2019-08-27.
 */
class PostgresqlTestRule : ExternalResource() {

    private lateinit var container: KPostgreSQLContainer
    private lateinit var dbi: Jdbi
    private lateinit var dataSource: DataSource

    override fun before() {
        container = KPostgreSQLContainer()
        container.start()
        dbi = getDataSource(container)
        dbi.installPlugin(SqlObjectPlugin())
        dbi.installPlugin(PostgresPlugin())
        executeCreateSql()
        Runtime.getRuntime().addShutdownHook(Thread {
            teardown()
        })
    }

    override fun after() {
        getDBI().useHandle<Exception> {
            it.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public")
        }
        executeCreateSql()
        LOG.info("Database is recreated")
    }

    private fun executeCreateSql() {
        dbi.useHandle<Exception> {
            it.createUpdate(PostgresqlTestRule::class.java.getResourceAsStream("/create.sql").bufferedReader().readText())
                    .execute()
        }
    }

    private fun getDataSource(container: KPostgreSQLContainer): Jdbi {
        return Jdbi.create(container.jdbcUrl, container.username, container.password)
    }

    fun getDBI(): Jdbi {
        return dbi
    }

    fun getDataSource(): DataSource {
        return dataSource
    }

    private fun teardown() {
        container.close()
        LOG.info("Docker container closed")
    }
}

class KPostgreSQLContainer : PostgreSQLContainer<KPostgreSQLContainer>("postgres:13")
