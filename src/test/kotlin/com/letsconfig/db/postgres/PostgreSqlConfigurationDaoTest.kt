package com.letsconfig.db.postgres

import com.letsconfig.common.PostgresqlTestRule
import com.letsconfig.db.AbstractConfigurationDaoTest
import com.letsconfig.db.ConfigurationDao
import org.junit.Rule

class PostgreSqlConfigurationDaoTest : AbstractConfigurationDaoTest() {

    @Rule
    @JvmField
    val postgresSqlRule = PostgresqlTestRule()

    override fun getDao(): ConfigurationDao {
        return PostgreSqlConfigurationDao(postgresSqlRule.getDBI())
    }
}