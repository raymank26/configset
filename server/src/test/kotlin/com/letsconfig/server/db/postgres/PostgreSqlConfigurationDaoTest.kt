package com.letsconfig.server.db.postgres

import com.letsconfig.server.common.PostgresqlTestRule
import com.letsconfig.server.db.AbstractConfigurationDaoTest
import com.letsconfig.server.db.ConfigurationDao
import org.junit.Rule

class PostgreSqlConfigurationDaoTest : AbstractConfigurationDaoTest() {

    @Rule
    @JvmField
    val postgresSqlRule = PostgresqlTestRule()

    override fun getDao(): ConfigurationDao {
        return PostgreSqlConfigurationDao(postgresSqlRule.getDBI())
    }
}