package com.configset.server.db.postgres

import com.configset.server.common.PostgresqlTestRule
import com.configset.server.db.AbstractConfigurationDaoTest
import com.configset.server.db.ConfigurationDao
import org.junit.Rule

class PostgreSqlConfigurationDaoTest : AbstractConfigurationDaoTest() {

    @Rule
    @JvmField
    val postgresSqlRule = PostgresqlTestRule()

    override fun getDao(): ConfigurationDao {
        return PostgreSqlConfigurationDao(postgresSqlRule.getDBI())
    }
}