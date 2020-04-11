package com.letsconfig.db.postgres

import com.letsconfig.PropertyCreateResult
import com.letsconfig.TEST_APP_NAME
import com.letsconfig.TEST_HOST
import com.letsconfig.common.PostgresqlTestRule
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*

class PostgreSqlConfigurationDaoTest {

    @Rule
    @JvmField
    val postgresSqlRule = PostgresqlTestRule()

    private lateinit var dao: PostgreSqlConfigurationDao

    @Before
    fun before() {
        dao = PostgreSqlConfigurationDao(postgresSqlRule.getDBI())
    }

    @Test
    fun testCreateApplication() {
        dao.createApplication(createRequestId(), TEST_APP_NAME)
        dao.listApplications().map { it.name } shouldBeEqualTo listOf(TEST_APP_NAME)
    }

    private fun createRequestId() = UUID.randomUUID().toString()

    @Test
    fun testCreateHost() {
        dao.createHost(createRequestId(), TEST_HOST)
        dao.listHosts().map { it.name } shouldBeEqualTo listOf(TEST_HOST)
    }

    @Test
    fun testCreateProperty() {
        dao.createApplication(createRequestId(), TEST_APP_NAME)
        dao.createHost(createRequestId(), TEST_HOST)
        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value1", 1) shouldBeEqualTo PropertyCreateResult.OK
        dao.listApplications().first().lastVersion shouldBeEqualTo 2
    }
}