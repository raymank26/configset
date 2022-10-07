package com.configset.server.db.postgres

import com.configset.server.common.PostgresqlTestRule
import com.configset.server.db.AbstractConfigurationDaoTest
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.DbHandleFactory
import org.junit.jupiter.api.extension.RegisterExtension

class PostgreSqlConfigurationDaoTest : AbstractConfigurationDaoTest() {

    override fun getDao(): ConfigurationDao {
        return PostgreSqlConfigurationDao(postgresSqlRule.getDBI())
    }

    override fun getDbHandleFactory(): DbHandleFactory {
        return PostgresDbHandleFactory(postgresSqlRule.getDBI())
    }

    companion object {
        @JvmStatic
        @RegisterExtension
        val postgresSqlRule = PostgresqlTestRule()
    }
}