package com.configset.server.functional.db.postgres

import com.configset.server.db.ConfigurationDao
import com.configset.server.db.DbHandleFactory
import com.configset.server.db.postgres.PostgreSqlConfigurationDao
import com.configset.server.db.postgres.PostgresDbHandleFactory
import com.configset.server.fixtures.PostgresqlExtension
import com.configset.server.functional.db.AbstractConfigurationDaoTest
import org.junit.jupiter.api.extension.RegisterExtension

class PostgreSqlConfigurationDaoTest : AbstractConfigurationDaoTest() {

    companion object {
        @JvmStatic
        @RegisterExtension
        val postgresSqlRule = PostgresqlExtension()
    }

    override fun getDao(): ConfigurationDao {
        return PostgreSqlConfigurationDao(postgresSqlRule.getDBI())
    }

    override fun getDbHandleFactory(): DbHandleFactory {
        return PostgresDbHandleFactory(postgresSqlRule.getDBI())
    }
}
