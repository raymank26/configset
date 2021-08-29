package com.configset.server.db.postgres

import com.configset.server.common.PostgresqlTestRule
import com.configset.server.db.AbstractConfigurationDaoTest
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.DbHandleFactory
import org.junit.Rule

class PostgreSqlConfigurationDaoTest : AbstractConfigurationDaoTest() {

    @Rule
    @JvmField
    val postgresSqlRule = PostgresqlTestRule()

    override fun getDao(): ConfigurationDao {
        @Suppress("UNCHECKED_CAST")
        return PostgreSqlConfigurationDao(postgresSqlRule.getDBI()) as ConfigurationDao
    }

    override fun getDbHandleFactory(): DbHandleFactory {
        return PostgresDbHandleFactory(postgresSqlRule.getDBI())
    }
}