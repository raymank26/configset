package com.configset.server.functional.db

import com.configset.sdk.ApplicationId
import com.configset.server.ApplicationED
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.SearchPropertyRequest
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.DbHandleFactory
import com.configset.server.db.PropertyItemED
import com.configset.server.fixtures.TEST_APP_NAME
import com.configset.server.fixtures.TEST_HOST
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldMatchAtLeastOneOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class AbstractConfigurationDaoTest {

    abstract fun getDao(): ConfigurationDao
    abstract fun getDbHandleFactory(): DbHandleFactory

    private lateinit var dao: ConfigurationDao
    private lateinit var dbHandleFactory: DbHandleFactory

    @BeforeEach
    fun setUpDao() {
        dao = getDao()
        dbHandleFactory = getDbHandleFactory()
    }

    @Test
    fun `should create application`() {
        test {
            // when
            createApp()

            // then
            appNames() shouldBeEqualTo listOf(TEST_APP_NAME)
        }
    }

    @Test
    fun `should delete application`() {
        test {
            // given
            createApp("test1")

            // when
            deleteApp("test1")

            // then
            appNames() shouldBeEqualTo listOf()
        }
    }

    @Test
    fun `should update application by id`() {
        test {
            // given
            createApp("test1")

            // when
            val appId = apps().find { it.name == "test1" }!!.id
            updateApp(appId, "test2")

            // then
            appNames() shouldBeEqualTo listOf("test2")
        }
    }

    @Test
    fun `should not return properties on deleted applications`() {
        test {
            // given
            createApp()
            createHost()
            updateProperty(name = "foo", value = "bar")

            // when
            val properties = properties()
            deleteApp(TEST_APP_NAME)

            // then
            val propertiesAfterDeletion = properties()
            properties.size shouldBeEqualTo 1
            propertiesAfterDeletion.shouldBeEmpty()
        }
    }

    @Test
    fun `should create host`() {
        test {
            // given
            createHost()

            // when
            appHosts() shouldBeEqualTo listOf(TEST_HOST)
        }
    }

    @Test
    fun `should update app version`() {
        test {
            // given
            createApp()
            createHost()
            updateProperty("name", "value")

            // when
            updateProperty("name", "valueNext", 1)

            // then
            apps().first().lastVersion shouldBeEqualTo 2
        }
    }

    @Test
    fun `property update should change previously created property`() {
        test {
            // given
            createApp()
            createHost()

            // when
            updateProperty("name", "value")
            updateProperty("name", "value1")

            // then
            val searchRes: List<PropertyItemED> = dao.searchProperties(
                SearchPropertyRequest(
                    TEST_APP_NAME,
                    null, null, null
                )
            )
            searchRes.find { it.name == "name" }!!.value shouldBeEqualTo "value1"
        }
    }

    @Test
    fun `property should be deleted`() {
        test {
            // given
            createApp()
            createHost()
            updateProperty("name", "value")

            // when
            deleteProperty("name") shouldBeEqualTo DeletePropertyResult.OK

            // then
            dao.listProperties(TEST_APP_NAME) shouldBeEqualTo listOf()

            val snapshot = dao.getConfigurationSnapshotList()
            snapshot.size shouldBeEqualTo 1

            val first = snapshot.first()
            first.deleted shouldBe true
            first.version shouldBeEqualTo 2

        }
    }

    @Test
    fun `should return conflict error on property deletion`() {
        test {
            // given
            createApp()
            createHost()

            // when
            updateProperty("name", "value")
            deleteProperty("name", version = 123) shouldBeEqualTo DeletePropertyResult.DeleteConflict

            // then
            dao.listProperties(TEST_APP_NAME).size shouldBeEqualTo 1
        }
    }

    @Test
    fun `should update deleted property`() {
        test {
            // given
            createApp()
            createHost()
            updateProperty("name", "value")
            deleteProperty("name")

            // when
            updateProperty("name", "value") // TODO: think about null version!

            // then
            val property = dao.searchProperties(SearchPropertyRequest(TEST_APP_NAME, "name", null, null)).first()
            property.version shouldBeEqualTo 3
        }

    }

    @Test
    fun `should list properties`() {
        test {
            // given
            createApp()
            createApp("test-app2")
            createHost()
            createHost("srvd2")

            // when
            updateProperty("name", "value")
            updateProperty("name", "value", host = "srvd2")
            updateProperty("name", "value", appName = "test-app2", host = "srvd2")

            // then
            dao.listProperties(TEST_APP_NAME) shouldBeEqualTo listOf("name")
        }
    }

    @Test
    fun `should search properties`() {
        test {
            // given
            createApp()
            createApp("test-app2")
            createHost()
            createHost("srvd2")

            // when
            updateProperty("name", "value")
            updateProperty("name2", "value2")
            updateProperty("name1", "value", host = "srvd2")

            // then
            dao.searchProperties(SearchPropertyRequest(null, "Nam", "Val", "SRVD1"))
                .map { it.name } shouldBeEqualTo listOf("name", "name2")
            dao.searchProperties(SearchPropertyRequest(null, "nam", "val", "srvd"))
                .map { it.name } shouldBeEqualTo listOf("name", "name2", "name1")
        }
    }

    @Test
    fun `should update property versions`() {
        test {
            // given
            createApp()
            createHost()
            createHost("srvd2")
            createHost("srvd3")

            // when
            updateProperty("name", "value")
            updateProperty("name", "value2", host = "srvd2")
            updateProperty("name", "value3", host = "srvd3")

            // then
            dao.readProperty(TEST_APP_NAME, TEST_HOST, "name")!!.apply {
                hostName shouldBeEqualTo "srvd1"
                applicationName shouldBeEqualTo TEST_APP_NAME
                name shouldBeEqualTo "name"
                value shouldBeEqualTo "value"
                version shouldBeEqualTo 1
            }

            dao.readProperty(TEST_APP_NAME, "srvd2", "name")!!.apply {
                hostName shouldBeEqualTo "srvd2"
                applicationName shouldBeEqualTo TEST_APP_NAME
                name shouldBeEqualTo "name"
                value shouldBeEqualTo "value2"
                version shouldBeEqualTo 2
            }

            dao.readProperty(TEST_APP_NAME, "srvd3", "name")!!.apply {
                hostName shouldBeEqualTo "srvd3"
                applicationName shouldBeEqualTo TEST_APP_NAME
                name shouldBeEqualTo "name"
                value shouldBeEqualTo "value3"
                version shouldBeEqualTo 3
            }
        }
    }

    @Test
    fun `should collect snapshot`() {
        test {
            // given
            createApp()
            createHost()
            createHost("srvd2")
            updateProperty("name", "value")
            updateProperty("name2", "value2")
            updateProperty("name", "value2", host = "srvd2")

            // then
            dao.getConfigurationSnapshotList()
                .shouldMatchAtLeastOneOf {
                    it.applicationName == TEST_APP_NAME
                            && it.name == "name"
                            && it.hostName == TEST_HOST
                            && it.version == 1L
                            && it.value == "value"
                }
                .shouldMatchAtLeastOneOf {
                    it.applicationName == TEST_APP_NAME
                            && it.name == "name2"
                            && it.hostName == TEST_HOST
                            && it.version == 2L
                            && it.value == "value2"
                }
                .shouldMatchAtLeastOneOf {
                    it.applicationName == TEST_APP_NAME
                            && it.name == "name"
                            && it.hostName == "srvd2"
                            && it.version == 3L
                            && it.value == "value2"
                }
        }
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

    fun createApp(name: String? = null) {
        dbHandleFactory.withHandle {
            dao.createApplication(it, name ?: TEST_APP_NAME)
        }
    }

    fun deleteApp(name: String) {
        dbHandleFactory.withHandle {
            dao.deleteApplication(it, name)
        }
    }

    fun updateApp(id: ApplicationId, name: String) {
        dbHandleFactory.withHandle {
            dao.updateApplication(it, id, name)
        }
    }

    fun createHost(name: String? = null) {
        dbHandleFactory.withHandle {
            dao.createHost(it, name ?: TEST_HOST) shouldBeEqualTo HostCreateResult.OK
        }
    }

    fun updateProperty(
        name: String,
        value: String,
        version: Long? = null,
        appName: String = TEST_APP_NAME,
        host: String = TEST_HOST,
    ) {
        dbHandleFactory.withHandle {
            val v = version ?: readProperty(name, app = appName, host = host)?.version
            dao.updateProperty(it, appName, name, value, v, host)
        }
    }

    fun readProperty(name: String, app: String = TEST_APP_NAME, host: String = TEST_HOST): PropertyItemED? {
        return dao.readProperty(app, host, name)
    }

    fun deleteProperty(name: String, version: Long? = null): DeletePropertyResult {
        val v = version ?: readProperty(name)!!.version
        return dbHandleFactory.withHandle {
            dao.deleteProperty(it, TEST_APP_NAME, TEST_HOST, name, v)
        }
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

    fun properties(): List<PropertyItemED> {
        return dao.getConfigurationSnapshotList()
    }
}
