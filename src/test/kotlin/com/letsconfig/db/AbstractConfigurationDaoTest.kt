package com.letsconfig.db

import com.letsconfig.CreateApplicationResult
import com.letsconfig.DeletePropertyResult
import com.letsconfig.HostCreateResult
import com.letsconfig.PropertyCreateResult
import com.letsconfig.TEST_APP_NAME
import com.letsconfig.TEST_HOST
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.util.*

abstract class AbstractConfigurationDaoTest {

    abstract fun getDao(): ConfigurationDao

    private lateinit var dao: ConfigurationDao

    @Before
    fun setUpDao() {
        dao = getDao()
    }

    @Test
    fun testCreateApplication() {
        val requestId = createRequestId()
        testIdempotent {
            dao.createApplication(requestId, TEST_APP_NAME) shouldBeEqualTo CreateApplicationResult.OK
        }
        dao.listApplications().map { it.name } shouldBeEqualTo listOf(TEST_APP_NAME)
    }

    private fun createRequestId() = UUID.randomUUID().toString()

    @Test
    fun testCreateHost() {
        val requestId = createRequestId()
        testIdempotent {
            dao.createHost(requestId, TEST_HOST) shouldBeEqualTo HostCreateResult.OK
        }
        dao.listHosts().map { it.name } shouldBeEqualTo listOf(TEST_HOST)
    }

    @Test
    fun testCreateProperty() {
        dao.createApplication(createRequestId(), TEST_APP_NAME)
        dao.createHost(createRequestId(), TEST_HOST)
        val updateRequest = createRequestId()
        testIdempotent {
            dao.updateProperty(updateRequest, TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        }
        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value1", 1) shouldBeEqualTo PropertyCreateResult.OK
        dao.listApplications().first().lastVersion shouldBeEqualTo 2
    }

    @Test
    fun testDeleteProperty() {
        dao.createApplication(createRequestId(), TEST_APP_NAME)
        dao.createHost(createRequestId(), TEST_HOST)
        val updateRequest = createRequestId()
        dao.updateProperty(updateRequest, TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK

        val requestId = createRequestId()
        testIdempotent {
            dao.deleteProperty(requestId, TEST_APP_NAME, TEST_HOST, "name") shouldBeEqualTo DeletePropertyResult.OK
        }
    }

    private fun testIdempotent(call: () -> Unit) {
        call.invoke()
        call.invoke()
    }

}
