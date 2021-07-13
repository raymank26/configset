package com.configset.server.db

import com.configset.server.CreateApplicationResult
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.PropertyCreateResult
import com.configset.server.PropertyItem
import com.configset.server.SearchPropertyRequest
import com.configset.server.TEST_APP_NAME
import com.configset.server.TEST_HOST
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
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
    fun testUpdateProperty() {
        dao.createApplication(createRequestId(), TEST_APP_NAME)
        dao.createHost(createRequestId(), TEST_HOST)
        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK

        val updateRequest = createRequestId()
        testIdempotent {
            dao.updateProperty(updateRequest, TEST_APP_NAME, TEST_HOST, "name", "value1", 1) shouldBeEqualTo PropertyCreateResult.OK
        }
        dao.listApplications().first().lastVersion shouldBeEqualTo 2
    }

    @Test
    fun testUpdateDoesntBreak() {
        dao.createApplication(createRequestId(), TEST_APP_NAME)
        dao.createHost(createRequestId(), TEST_HOST)
        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "foobar", "value2", null) shouldBeEqualTo PropertyCreateResult.OK

        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value1", 1) shouldBeEqualTo PropertyCreateResult.OK

        val searchRes: List<PropertyItem.Updated> = dao.searchProperties(SearchPropertyRequest(TEST_APP_NAME, null, null, null))
        searchRes.find { it.name == "name" }!!.value shouldBeEqualTo "value1"
        searchRes.find { it.name == "foobar" }!!.value shouldBeEqualTo "value2"
    }

    @Test
    fun testDeleteProperty() {
        dao.createApplication(createRequestId(), TEST_APP_NAME)
        dao.createHost(createRequestId(), TEST_HOST)

        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.deleteProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", 1) shouldBeEqualTo DeletePropertyResult.OK
        dao.listProperties(TEST_APP_NAME) shouldBeEqualTo listOf()

        val snapshot = dao.getConfigurationSnapshotList()
        snapshot.size shouldBeEqualTo 1

        val first = snapshot.first()
        first shouldBeInstanceOf PropertyItem.Deleted::class.java
        first.version shouldBeEqualTo 2
    }

    @Test
    fun testDeleteConflict() {
        dao.createApplication(createRequestId(), TEST_APP_NAME)
        dao.createHost(createRequestId(), TEST_HOST)

        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.deleteProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", 1123) shouldBeEqualTo DeletePropertyResult.DeleteConflict
        dao.listProperties(TEST_APP_NAME).size shouldBeEqualTo 1
    }

    @Test
    fun testUpdateAfterDelete() {
        dao.createApplication(createRequestId(), TEST_APP_NAME)
        dao.createHost(createRequestId(), TEST_HOST)

        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.deleteProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", 1) shouldBeEqualTo DeletePropertyResult.OK
        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value2", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.searchProperties(SearchPropertyRequest(TEST_APP_NAME, "name", null, null)).first().version shouldBeEqualTo 3

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

        dao.searchProperties(SearchPropertyRequest(null, "Nam", "Val", "SRVD1")).map { it.name } shouldBeEqualTo listOf("name", "name2")
        dao.searchProperties(SearchPropertyRequest(null, "nam", "val", "srvd")).map { it.name } shouldBeEqualTo listOf("name", "name2", "name1")
    }

    @Test
    fun readProperty() {
        dao.createApplication(createRequestId(), TEST_APP_NAME) shouldBeEqualTo CreateApplicationResult.OK
        dao.createHost(createRequestId(), TEST_HOST) shouldBeEqualTo HostCreateResult.OK
        dao.createHost(createRequestId(), "srvd2") shouldBeEqualTo HostCreateResult.OK
        dao.createHost(createRequestId(), "srvd3") shouldBeEqualTo HostCreateResult.OK

        dao.updateProperty(createRequestId(), TEST_APP_NAME, TEST_HOST, "name", "value", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(createRequestId(), TEST_APP_NAME, "srvd2", "name", "value2", null) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(createRequestId(), TEST_APP_NAME, "srvd3", "name", "value3", null) shouldBeEqualTo PropertyCreateResult.OK

        dao.readProperty(TEST_APP_NAME, TEST_HOST, "name") shouldBeEqualTo PropertyItem.Updated(hostName = "srvd1",
                applicationName = TEST_APP_NAME, name = "name", value = "value", version = 1)

        dao.readProperty(TEST_APP_NAME, "srvd2", "name") shouldBeEqualTo PropertyItem.Updated(hostName = "srvd2",
                applicationName = TEST_APP_NAME, name = "name", value = "value2", version = 2)

        dao.readProperty(TEST_APP_NAME, "srvd3", "name") shouldBeEqualTo PropertyItem.Updated(hostName = "srvd3",
                applicationName = TEST_APP_NAME, name = "name", value = "value3", version = 3)
    }

    @Test
    fun testSnapshot() {
        dao.createApplication(createRequestId(), TEST_APP_NAME) shouldBeEqualTo CreateApplicationResult.OK
        dao.createHost(createRequestId(), TEST_HOST) shouldBeEqualTo HostCreateResult.OK
        dao.createHost(createRequestId(), "srvd2") shouldBeEqualTo HostCreateResult.OK
        dao.updateProperty(
            createRequestId(),
            TEST_APP_NAME,
            TEST_HOST,
            "name",
            "value",
            null
        ) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(
            createRequestId(),
            TEST_APP_NAME,
            TEST_HOST,
            "name2",
            "value2",
            null
        ) shouldBeEqualTo PropertyCreateResult.OK
        dao.updateProperty(
            createRequestId(),
            TEST_APP_NAME,
            "srvd2",
            "name",
            "value2",
            null
        ) shouldBeEqualTo PropertyCreateResult.OK

        dao.getConfigurationSnapshotList().sortedBy { it.name }.sortedBy { it.hostName } shouldBeEqualTo listOf(
            PropertyItem.Updated(TEST_APP_NAME, "name", TEST_HOST, 1, "value"),
            PropertyItem.Updated(TEST_APP_NAME, "name2", TEST_HOST, 2, "value2"),
                PropertyItem.Updated(TEST_APP_NAME, "name", "srvd2", 3, "value2")
        )
    }

    private fun testIdempotent(call: () -> Unit) {
        call.invoke()
        call.invoke()
    }
}
