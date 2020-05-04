package com.letsconfig.server.db

import com.letsconfig.server.CreateApplicationResult
import com.letsconfig.server.DeletePropertyResult
import com.letsconfig.server.HostCreateResult
import com.letsconfig.server.PropertyCreateResult
import com.letsconfig.server.SearchPropertyRequest
import com.letsconfig.server.ShowPropertyItem
import com.letsconfig.server.TEST_APP_NAME
import com.letsconfig.server.TEST_HOST
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

    @Test
    fun listProperties() {
        dao.createApplication(createRequestId(), TEST_APP_NAME) shouldBeEqualTo CreateApplicationResult.OK
        dao.createApplication(createRequestId(), "test-app2") shouldBeEqualTo CreateApplicationResult.OK
        dao.createHost(createRequestId(), TEST_HOST) shouldBeEqualTo HostCreateResult.OK
        dao.createHost(createRequestId(), "srvd2")

        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(createRequestId(), TEST_APP_NAME, "srvd2", "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(createRequestId(), "test-app2", TEST_HOST, "name2", "value", null) shouldBeEqualTo PropertyCreateResult.OK

        dao.listProperties(TEST_APP_NAME) shouldBeEqualTo listOf("name")
    }

    @Test
    fun searchProperties() {
        dao.createApplication(createRequestId(), TEST_APP_NAME) shouldBeEqualTo CreateApplicationResult.OK
        dao.createApplication(createRequestId(), "test-app2") shouldBeEqualTo CreateApplicationResult.OK
        dao.createHost(createRequestId(), TEST_HOST) shouldBeEqualTo HostCreateResult.OK
        dao.createHost(createRequestId(), "srvd2") shouldBeEqualTo HostCreateResult.OK

        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name2", "value2", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(createRequestId(), TEST_APP_NAME, "srvd2", "name1", "value", null) shouldBeEqualTo PropertyCreateResult.OK

        dao.searchProperties(SearchPropertyRequest(null, "nam", "val", "srvd1")).map { it.name } shouldBeEqualTo listOf("name", "name2")
        dao.searchProperties(SearchPropertyRequest(null, "nam", "val", "srvd")).map { it.name } shouldBeEqualTo listOf("name", "name2", "name1")
    }

    @Test
    fun showProperty() {
        dao.createApplication(createRequestId(), TEST_APP_NAME) shouldBeEqualTo CreateApplicationResult.OK
        dao.createHost(createRequestId(), TEST_HOST) shouldBeEqualTo HostCreateResult.OK
        dao.createHost(createRequestId(), "srvd2") shouldBeEqualTo HostCreateResult.OK
        dao.createHost(createRequestId(), "srvd3") shouldBeEqualTo HostCreateResult.OK

        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(createRequestId(), TEST_APP_NAME, "srvd2", "name", "value2", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(createRequestId(), TEST_APP_NAME, "srvd3", "name", "value3", null) shouldBeEqualTo PropertyCreateResult.OK

        dao.showProperty(TEST_APP_NAME, "name") shouldBeEqualTo listOf(
                ShowPropertyItem(hostName = "srvd1", propertyName = "name", propertyValue = "value"),
                ShowPropertyItem(hostName = "srvd2", propertyName = "name", propertyValue = "value2"),
                ShowPropertyItem(hostName = "srvd3", propertyName = "name", propertyValue = "value3"))
    }

    private fun testIdempotent(call: () -> Unit) {
        call.invoke()
        call.invoke()
    }

}
