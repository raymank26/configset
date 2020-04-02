package com.letsconfig.db.postgres

import com.letsconfig.CreateApplicationResult
import com.letsconfig.DeletePropertyResult
import com.letsconfig.HostCreateResult
import com.letsconfig.PropertyCreateResult
import com.letsconfig.PropertyItem
import com.letsconfig.db.ConfigurationDao
import com.letsconfig.extension.createLogger
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.transaction.TransactionIsolationLevel
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate


class PostgreSqlConfigurationDao(private val dbi: Jdbi) : ConfigurationDao {

    val logger = createLogger()

    override fun listApplications(): List<String> {
        return dbi.withExtension<List<String>, JdbiAccess, java.lang.Exception>(JdbiAccess::class.java) { access ->
            access.listApplications().map { it.name }
        }
    }

    override fun createApplication(appName: String): CreateApplicationResult {
        return dbi.inTransaction<CreateApplicationResult, Exception>(TransactionIsolationLevel.SERIALIZABLE) { handle ->
            val access = handle.attach<JdbiAccess>(JdbiAccess::class.java)
            val createdApp = access.getApplicationByName(appName)
            if (createdApp != null) {
                CreateApplicationResult.ApplicationAlreadyExists
            } else {
                access.insertApplication(ApplicationED(null, appName, 0))
                CreateApplicationResult.OK
            }
        }
    }

    override fun createHost(hostName: String): HostCreateResult {
        return dbi.inTransaction<HostCreateResult, Exception>(TransactionIsolationLevel.SERIALIZABLE) { handle ->
            val access = handle.attach<JdbiAccess>(JdbiAccess::class.java)
            val host = access.getHostByName(hostName)
            if (host != null) {
                HostCreateResult.HostAlreadyExists
            } else {
                access.insertHost(HostED(null, hostName))
                HostCreateResult.OK
            }
        }
    }

    override fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult {
        return dbi.inTransaction<PropertyCreateResult, java.lang.Exception>(TransactionIsolationLevel.SERIALIZABLE) { handle ->
            val access = handle.attach<JdbiAccess>(JdbiAccess::class.java)
            val app = access.getApplicationByName(appName)
                    ?: return@inTransaction PropertyCreateResult.ApplicationNotFound
            val host = access.getHostByName(hostName) ?: return@inTransaction PropertyCreateResult.HostNotFound
            val property = access.getProperty(appName, host.id!!, app.id!!)

            if (property == null && version == null) {
                access.insertProperty(PropertyItemED(null, propertyName, value, host.id, app.id, app.lastVersion + 1, false))
                PropertyCreateResult.OK
            } else if (property != null) {
                if (property.version != version) {
                    PropertyCreateResult.UpdateConflict
                } else {
                    access.updateProperty(PropertyItemED(property.id, propertyName, value, host.id, app.id, app.lastVersion + 1, false))
                    PropertyCreateResult.OK
                }
            } else {
                PropertyCreateResult.UpdateConflict
            }
        }
    }

    override fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult {
        return dbi.inTransaction<DeletePropertyResult, java.lang.Exception>(TransactionIsolationLevel.SERIALIZABLE) { handle ->
            val access = handle.attach<JdbiAccess>(JdbiAccess::class.java)
            val app = access.getApplicationByName(appName)
                    ?: return@inTransaction DeletePropertyResult.PropertyNotFound
            val host = access.getHostByName(hostName) ?: return@inTransaction DeletePropertyResult.PropertyNotFound
            val property = access.getProperty(appName, host.id!!, app.id!!)
                    ?: return@inTransaction DeletePropertyResult.PropertyNotFound
            access.markPropertyAsDeleted(property.id!!)
            DeletePropertyResult.OK
        }
    }

    override fun getConfigurationSnapshotList(): List<PropertyItem> {
        return dbi.inTransaction<List<PropertyItem>, java.lang.Exception>(TransactionIsolationLevel.SERIALIZABLE) { handle ->
            val access = handle.attach<JdbiAccess>(JdbiAccess::class.java)
            val properties = access.selectAllProperties()
            val hostIds = properties.map { it.hostId }
            val appIds = properties.map { it.appId }
            val hosts: Map<Long, HostED> = access.getHostByIds(hostIds).associateBy { it.id!! }
            val apps: Map<Long, ApplicationED> = access.getApplicationsByIds(appIds).associateBy { it.id!! }
            val res = mutableListOf<PropertyItem>()

            for (property in properties) {
                val app = apps[property.appId]
                if (app == null) {
                    logger.warn("Unable to find application by id = {}", property.appId)
                    continue
                }
                val host = hosts[property.hostId]
                if (host == null) {
                    logger.warn("Unable to find host by id = {}", property.hostId)
                    continue
                }
                val propertyItem = if (property.deleted) {
                    PropertyItem.Deleted(app.name, property.name, host.name, property.version)
                } else {
                    PropertyItem.Updated(app.name, property.name, property.value, property.version, host.name)
                }
                res.add(propertyItem)
            }
            res
        }
    }
}

private interface JdbiAccess {
    @SqlQuery("select * from ConfigurationApplication")
    fun listApplications(): List<ApplicationED>

    @SqlQuery
    fun getApplicationsByIds(ids: List<Long>): List<ApplicationED>

    @SqlQuery("select * from ConfigurationApplication where name = :name")
    fun getApplicationByName(name: String): ApplicationED?

    @SqlUpdate("insert into ConfigurationApplication (name) values (:name)")
    fun insertApplication(app: ApplicationED)

    @SqlUpdate("insert into ConfigurationHost (name) values (:host)")
    fun insertHost(host: HostED)

    @SqlQuery("select * from ConfigurationHost where name = :name")
    fun getHostByName(name: String): HostED?

    @SqlQuery
    fun getHostByIds(ids: List<Long>): List<HostED>

    @SqlQuery("select * from ConfigurationProperty where name = :name AND hostId = :hostId AND appId = :appId")
    fun getProperty(name: String, hostId: Long, appId: Long): PropertyItemED?

    @SqlUpdate("insert into ConfigurationProperty (appId, hostId, name, value) values (:appId, :hostId, :name, :value)")
    fun insertProperty(property: PropertyItemED)

    @SqlUpdate("insert into ConfigurationProperty (appId, hostId, name, value) values (:appId, :hostId, :name, :value)")
    fun updateProperty(property: PropertyItemED)

    @SqlUpdate("update ConfigurationProperty SET deleted = true where id = :id")
    fun markPropertyAsDeleted(id: Long)

    @SqlQuery("select * from ConfigurationProperty")
    fun selectAllProperties(): List<PropertyItemED>
}

private data class ApplicationED(val id: Long?, val name: String, val lastVersion: Long)

private data class HostED(val id: Long?, val name: String)

private data class PropertyItemED(val id: Long?, val name: String, val value: String, val hostId: Long,
                                  val appId: Long, val version: Long, val deleted: Boolean)

