package com.configset.server.fixtures

import com.configset.server.db.postgres.DbMigrator
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer

private val LOG = LoggerFactory.getLogger(PostgresqlTestRule::class.java)

/**
 * @author anton.ermak
 * Date: 2019-08-27.
 */
class PostgresqlTestRule : BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private lateinit var container: KPostgreSQLContainer
    private lateinit var dbi: Jdbi

    override fun beforeAll(context: ExtensionContext?) {
        container = KPostgreSQLContainer()
        container.start()
        dbi = getDataSource(container)
        dbi.installPlugin(SqlObjectPlugin())
        dbi.installPlugin(PostgresPlugin())
    }

    override fun beforeEach(context: ExtensionContext?) {
        DbMigrator(dbi).migrate()
    }

    override fun afterAll(context: ExtensionContext?) {
        container.close()
        LOG.info("Docker container closed")
    }

    override fun afterEach(context: ExtensionContext?) {
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
}

class KPostgreSQLContainer : PostgreSQLContainer<KPostgreSQLContainer>("postgres:13")
