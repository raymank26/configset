package com.configset.server.db

import com.configset.server.ApplicationED
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.PropertyItem
import com.configset.server.SearchPropertyRequest
import com.configset.server.db.common.DbHandle
import com.configset.test.fixtures.TEST_APP_NAME
import com.configset.test.fixtures.TEST_HOST
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

abstract class AbstractConfigurationDaoTest {

    abstract fun getDao(): ConfigurationDao
    abstract fun getDbHandleFactory(): DbHandleFactory

    private lateinit var dao: ConfigurationDao
    private lateinit var dbHandleFactory: DbHandleFactory

    @Before
    fun setUpDao() {
        dao = getDao()
        dbHandleFactory = getDbHandleFactory()
    }

    @Test
    fun testCreateApplication() {
        test {
            atomic {
                createApp()
            }
            appNames() shouldBeEqualTo listOf(TEST_APP_NAME)
        }
    }

    @Test
    fun testCreateHost() {
        test {
            atomic {
                createHost()
            }
            appHosts() shouldBeEqualTo listOf(TEST_HOST)
        }
    }

    @Test
    fun testCreateProperty() {
        test {
            atomic {
                createApp()
                createHost()
                updateProperty("name", "value")
            }
            atomic {
                updateProperty("name", "valueNext", 1)
            }
            apps().first().lastVersion shouldBeEqualTo 2
        }
    }

    @Test
    fun testUpdateDoesntBreak() {
        test {
            atomic {
                createApp()
                createHost()
                updateProperty("name", "value")
                updateProperty("foobar", "value2")
            }
            atomic {
                readProperty("name")
                updateProperty("name", "value1")
            }
        }
        val searchRes: List<PropertyItem.Updated> =
            dao.searchProperties(SearchPropertyRequest(TEST_APP_NAME, null, null, null))
        searchRes.find { it.name == "name" }!!.value shouldBeEqualTo "value1"
        searchRes.find { it.name == "foobar" }!!.value shouldBeEqualTo "value2"
    }

    @Test
    fun testDeleteProperty() {
        test {
            atomic {
                createApp()
                createHost()
                updateProperty("name", "value")
                deleteProperty("name") shouldBeEqualTo DeletePropertyResult.OK
            }
        }
        dao.listProperties(TEST_APP_NAME) shouldBeEqualTo listOf()

        val snapshot = dao.getConfigurationSnapshotList()
        snapshot.size shouldBeEqualTo 1

        val first = snapshot.first()
        first shouldBeInstanceOf PropertyItem.Deleted::class.java
        first.version shouldBeEqualTo 2
    }

    @Test
    fun testDeleteConflict() {
        test {
            atomic {
                createApp()
                createHost()
            }
            atomic {
                updateProperty("name", "value")
                deleteProperty("name", version = 123) shouldBeEqualTo DeletePropertyResult.DeleteConflict
            }
        }
        dao.listProperties(TEST_APP_NAME).size shouldBeEqualTo 1
    }

    @Test
    fun testUpdateAfterDelete() {
        test {
            atomic {
                createApp()
                createHost()
            }
            atomic {
                updateProperty("name", "value")
                deleteProperty("name")
                updateProperty("name", "value") // TODO: think about null version!
            }
        }
        dao.searchProperties(SearchPropertyRequest(TEST_APP_NAME, "name", null, null)).first().version shouldBeEqualTo 3
    }

    @Test
    fun listProperties() {
        test {
            atomic {
                createApp()
                createApp("test-app2")
                createHost()
                createHost("srvd2")
                updateProperty("name", "value")
                updateProperty("name", "value", host = "srvd2")
                updateProperty("name", "value", appName = "test-app2", host = "srvd2")
            }
        }

        dao.listProperties(TEST_APP_NAME) shouldBeEqualTo listOf("name")
    }

    @Test
    fun searchProperties() {
        test {
            atomic {
                createApp()
                createApp("test-app2")
                createHost()
                createHost("srvd2")
                updateProperty("name", "value")
                updateProperty("name2", "value2")
                updateProperty("name1", "value", host = "srvd2")
            }
        }

        dao.searchProperties(SearchPropertyRequest(null, "Nam", "Val", "SRVD1")).map { it.name } shouldBeEqualTo listOf(
            "name",
            "name2")
        dao.searchProperties(SearchPropertyRequest(null, "nam", "val", "srvd"))
            .map { it.name } shouldBeEqualTo listOf("name", "name2", "name1")
    }

    @Test
    fun readProperty() {
        test {
            atomic {
                createApp()
                createHost()
                createHost("srvd2")
                createHost("srvd3")
                updateProperty("name", "value")
                updateProperty("name", "value2", host = "srvd2")
                updateProperty("name", "value3", host = "srvd3")
            }
        }

        dao.readProperty(TEST_APP_NAME, TEST_HOST, "name") shouldBeEqualTo PropertyItem.Updated(hostName = "srvd1",
            applicationName = TEST_APP_NAME, name = "name", value = "value", version = 1)

        dao.readProperty(TEST_APP_NAME, "srvd2", "name") shouldBeEqualTo PropertyItem.Updated(hostName = "srvd2",
            applicationName = TEST_APP_NAME, name = "name", value = "value2", version = 2)

        dao.readProperty(TEST_APP_NAME, "srvd3", "name") shouldBeEqualTo PropertyItem.Updated(hostName = "srvd3",
            applicationName = TEST_APP_NAME, name = "name", value = "value3", version = 3)
    }

    @Test
    fun testSnapshot() {
        test {
            atomic {
                createApp()
                createHost()
                createHost("srvd2")
                updateProperty("name", "value")
                updateProperty("name2", "value2")
                updateProperty("name", "value2", host = "srvd2")
            }
        }

        dao.getConfigurationSnapshotList().sortedBy { it.name }.sortedBy { it.hostName } shouldBeEqualTo listOf(
            PropertyItem.Updated(TEST_APP_NAME, "name", TEST_HOST, 1, "value"),
            PropertyItem.Updated(TEST_APP_NAME, "name2", TEST_HOST, 2, "value2"),
            PropertyItem.Updated(TEST_APP_NAME, "name", "srvd2", 3, "value2")
        )
    }

    private fun test(f: TestDsl.() -> Unit) {
        val t = TestDsl(dbHandleFactory, dao)
        f(t)
    }
}

class TestDsl(
    private val dbHandleFactory: DbHandleFactory,
    private val dao: ConfigurationDao,
) {

    @Deprecated("Incorrect context", level = DeprecationLevel.ERROR)
    fun DbHandle.atomic(handle: DbHandle.() -> Unit) {
    }

    fun atomic(handle: DbHandle.() -> Unit) {
        dbHandleFactory.withHandle {
            it.handle()
        }
    }

    fun DbHandle.createApp(name: String? = null) {
        dao.createApplication(this, name ?: TEST_APP_NAME)
    }

    fun DbHandle.createHost(name: String? = null) {
        dao.createHost(this, name ?: TEST_HOST) shouldBeEqualTo HostCreateResult.OK
    }

    fun DbHandle.updateProperty(
        name: String,
        value: String,
        version: Long? = null,
        appName: String = TEST_APP_NAME,
        host: String = TEST_HOST,
    ) {
        val v = version ?: readProperty(name, app = appName, host = host)?.version
        dao.updateProperty(this, appName, name, value, v, host)
    }

    fun readProperty(name: String, app: String = TEST_APP_NAME, host: String = TEST_HOST): PropertyItem? {
        return dao.readProperty(app, host, name)
    }

    fun DbHandle.deleteProperty(name: String, version: Long? = null): DeletePropertyResult {
        val v = version ?: readProperty(name)!!.version
        return dao.deleteProperty(this, TEST_APP_NAME, TEST_HOST, name, v)
    }

    fun apps(): List<ApplicationED> {
        return dao.listApplications()
    }

    fun appNames(): List<String> {
        return dao.listApplications().map { it.name }
    }

    fun appHosts(): List<String> {
        return dao.listHosts().map { it.name }
    }
}
