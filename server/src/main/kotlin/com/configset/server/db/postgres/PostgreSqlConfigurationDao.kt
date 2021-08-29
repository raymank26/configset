package com.configset.server.db.postgres

import com.configset.sdk.extension.createLogger
import com.configset.server.ApplicationED
import com.configset.server.CreateApplicationResult
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.HostED
import com.configset.server.PropertyCreateResult
import com.configset.server.PropertyItem
import com.configset.server.SearchPropertyRequest
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.common.DbHandle
import com.configset.server.db.common.containsLowerCase
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.core.transaction.TransactionIsolationLevel
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.sql.ResultSet


class PostgreSqlConfigurationDao(private val dbi: Jdbi) : ConfigurationDao {

    private val logger = createLogger()

    init {
        dbi.registerRowMapper(ApplicationEDRowMapper())
        dbi.registerRowMapper(HostEDRowMapper())
        dbi.registerRowMapper(PropertyItemEDMapper())
        dbi.registerRowMapper(PropertyItemEDMapper())
    }

    override fun initialize() {
    }

    override fun listApplications(): List<ApplicationED> {
        return dbi.withExtension<List<ApplicationED>, JdbiAccess> { access ->
            access.listApplications()
        }
    }

    override fun createApplication(handle: DbHandle, appName: String): CreateApplicationResult {
        return processMutable(handle) { access ->
            val createdApp = access.getApplicationByName(appName)
            if (createdApp != null) {
                CreateApplicationResult.ApplicationAlreadyExists
            } else {
                access.insertApplication(appName, System.currentTimeMillis())
                CreateApplicationResult.OK
            }
        }
    }

    override fun createHost(handle: DbHandle, hostName: String): HostCreateResult {
        return processMutable(handle) { access ->
            val host = access.getHostByName(hostName)
            if (host != null) {
                HostCreateResult.HostAlreadyExists
            } else {
                access.insertHost(hostName, System.currentTimeMillis())
                HostCreateResult.OK
            }
        }
    }

    override fun listHosts(): List<HostED> {
        return dbi.withExtension<List<HostED>, JdbiAccess, Exception>(JdbiAccess::class.java) { access ->
            access.listHosts()
        }
    }

    override fun readProperty(applicationName: String, hostName: String, propertyName: String): PropertyItem? {
        return dbi.withExtension<PropertyItem, JdbiAccess, Exception>(JdbiAccess::class.java) { access ->
            val app = access.listApplications().find { it.name == applicationName } ?: return@withExtension null
            val host = access.listHosts().find { it.name == hostName } ?: return@withExtension null
            val property = access.readProperty(app.id!!, propertyName, host.id!!)
            when {
                property == null -> null
                property.deleted -> PropertyItem.Deleted(applicationName, propertyName, hostName, property.version)
                else -> PropertyItem.Updated(applicationName, propertyName, hostName, property.version, property.value)
            }
        }
    }

    override fun updateProperty(
        handle: DbHandle,
        appName: String,
        propertyName: String,
        value: String,
        version: Long?,
        hostName: String,
    ): PropertyCreateResult {
        return processMutable(handle) cb@{ access ->
            val app = access.getApplicationByName(appName)
                ?: return@cb PropertyCreateResult.ApplicationNotFound
            val host = access.getHostByName(hostName)
                ?: return@cb PropertyCreateResult.HostNotFound
            val property = access.getProperty(propertyName, host.id!!, app.id!!)

            val ct = System.currentTimeMillis()
            if (property == null && version == null) {
                access.insertProperty(propertyName, value, app.lastVersion + 1, app.id, host.id, ct)
                access.incrementAppVersion(app.id)
                return@cb PropertyCreateResult.OK
            } else if (property != null) {
                if (!property.deleted && property.version != version) {
                    return@cb PropertyCreateResult.UpdateConflict
                } else {
                    access.updateProperty(
                        property.id!!,
                        value,
                        app.lastVersion + 1,
                        false,
                        ct,
                        app.id,
                        propertyName,
                        host.id
                    )
                    access.incrementAppVersion(app.id)
                    return@cb PropertyCreateResult.OK
                }
            } else {
                return@cb PropertyCreateResult.UpdateConflict
            }
        }
    }

    override fun deleteProperty(
        handle: DbHandle,
        appName: String,
        hostName: String,
        propertyName: String,
        version: Long,
    ): DeletePropertyResult {
        return processMutable(handle) cb@{ access ->
            val app = access.getApplicationByName(appName)
                ?: return@cb DeletePropertyResult.PropertyNotFound
            val host = access.getHostByName(hostName)
                ?: return@cb DeletePropertyResult.PropertyNotFound
            val property = access.getProperty(propertyName, host.id!!, app.id!!)
                ?: return@cb DeletePropertyResult.PropertyNotFound
            if (property.version != version) {
                return@cb DeletePropertyResult.DeleteConflict
            }
            access.markPropertyAsDeleted(property.id!!, app.lastVersion + 1)
            access.incrementAppVersion(app.id)
            DeletePropertyResult.OK
        }
    }

    override fun getConfigurationSnapshotList(): List<PropertyItem> {
        return dbi.inTransaction<List<PropertyItem>, Exception>(TransactionIsolationLevel.SERIALIZABLE) { handle ->
            val access = handle.attach(JdbiAccess::class.java)
            val properties = access.selectAllProperties()
            val hosts: Map<Long, HostED> = access.listHosts().associateBy { it.id!! }
            val apps: Map<Long, ApplicationED> = access.listApplications().associateBy { it.id!! }
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
                    PropertyItem.Updated(app.name, property.name, host.name, property.version, property.value)
                }
                res.add(propertyItem)
            }
            res
        }
    }

    override fun searchProperties(searchPropertyRequest: SearchPropertyRequest): List<PropertyItem.Updated> {
        return dbi.withExtension<List<PropertyItem.Updated>, JdbiAccess, Exception>(JdbiAccess::class.java) { access ->
            val hosts = access.listHosts().associateBy { it.id }
            val apps = access.listApplications().associateBy { it.id }

            access.selectAllProperties()
                .filter { !it.deleted }
                .mapNotNull { property ->
                    val hostName = hosts[property.hostId]?.name ?: return@mapNotNull null
                    val appName = apps[property.appId]?.name ?: return@mapNotNull null
                    if (searchPropertyRequest.applicationName != null && appName != searchPropertyRequest.applicationName) {
                        return@mapNotNull null
                    }
                    if (searchPropertyRequest.hostNameQuery != null && !containsLowerCase(
                            hostName,
                            searchPropertyRequest.hostNameQuery
                        )
                    ) {
                        return@mapNotNull null
                    }
                    if (searchPropertyRequest.propertyNameQuery != null && !containsLowerCase(
                            property.name,
                            searchPropertyRequest.propertyNameQuery
                        )
                    ) {
                        return@mapNotNull null
                    }
                    if (searchPropertyRequest.propertyValueQuery != null && !containsLowerCase(
                            property.value,
                            searchPropertyRequest.propertyValueQuery
                        )
                    ) {
                        return@mapNotNull null
                    }
                    PropertyItem.Updated(appName, property.name, hostName, property.version, property.value)
                }
        }
    }

    override fun listProperties(applicationName: String): List<String> {
        return dbi.withExtension<List<String>, JdbiAccess, Exception>(JdbiAccess::class.java) { access ->
            val apps = access.listApplications().associateBy { it.id }
            access.selectAllProperties()
                .filter { !it.deleted }
                .mapNotNull {
                    val appName = apps[it.appId]?.name ?: return@mapNotNull null
                    if (applicationName == appName) it.name else null
                }.distinct()
        }
    }

    private fun <K> processMutable(
        handle: DbHandle,
        callback: (access: JdbiAccess) -> K,
    ): K {
        val access = handle.getApi(JdbiAccess::class.java)
        return callback(access)
    }
}

private interface JdbiAccess {

    @SqlQuery("select * from ConfigurationApplication")
    fun listApplications(): List<ApplicationED>

    @SqlQuery("select * from ConfigurationApplication where name = :name")
    fun getApplicationByName(@Bind("name") name: String): ApplicationED?

    @SqlUpdate("insert into ConfigurationApplication (name, version, createdMs, modifiedMs) values (:name, 0, :createdMs, :createdMs)")
    fun insertApplication(@Bind("name") name: String, @Bind("createdMs") createdMs: Long)

    @SqlUpdate("update ConfigurationApplication set version = version + 1 WHERE id = :id")
    fun incrementAppVersion(@Bind("id") id: Long)

    @SqlUpdate("insert into ConfigurationHost (name, createdMs, modifiedMs) values (:name, :createdMs, :createdMs)")
    fun insertHost(@Bind("name") name: String, @Bind("createdMs") createdMs: Long)

    @SqlQuery("select * from ConfigurationHost where name = :name")
    fun getHostByName(@Bind("name") name: String): HostED?

    @SqlQuery("select * from ConfigurationHost")
    fun listHosts(): List<HostED>

    @SqlQuery("select * from ConfigurationProperty where name = :name AND hostId = :hostId AND appId = :appId")
    fun getProperty(
        @Bind("name") name: String,
        @Bind("hostId") hostId: Long,
        @Bind("appId") appId: Long
    ): PropertyItemED?

    @SqlUpdate(
        "insert into ConfigurationProperty (name, value, version, appId, hostId, deleted, createdMs, modifiedMs)" +
                " values (:name, :value, :version, :appId, :hostId, false, :createdMs, :createdMs)"
    )
    fun insertProperty(
        @Bind("name") name: String, @Bind("value") value: String, @Bind("version") version: Long,
        @Bind("appId") appId: Long, @Bind("hostId") hostId: Long, @Bind("createdMs") modifiedMs: Long
    )

    @SqlUpdate(
        "update ConfigurationProperty set value = :value, version = :version, deleted = :deleted, modifiedMs = :modifiedMs " +
                "where appId = :appId and name = :name and hostId = :hostId"
    )
    fun updateProperty(
        @Bind("id") id: Long, @Bind("value") value: String, @Bind("version") version: Long,
        @Bind("deleted") deleted: Boolean, @Bind("modifiedMs") modifiedMs: Long, @Bind("appId") appId: Long,
        @Bind("name") name: String, @Bind("hostId") hostId: Long
    )

    @SqlUpdate("update ConfigurationProperty SET deleted = true, version = :version where id = :id")
    fun markPropertyAsDeleted(@Bind("id") id: Long, @Bind("version") version: Long)

    @SqlQuery("select * from ConfigurationProperty")
    fun selectAllProperties(): List<PropertyItemED>

    @SqlQuery("select * from ConfigurationProperty where appId = :appId and name = :name and hostId = :hostId")
    fun readProperty(
        @Bind("appId") appId: Long,
        @Bind("name") name: String,
        @Bind("hostId") hostId: Long
    ): PropertyItemED?
}

private data class PropertyItemED(
    val id: Long?, val name: String, val value: String, val hostId: Long,
    val appId: Long, val version: Long, val deleted: Boolean, val createdMs: Long, val modifiedMs: Long
)


private class ApplicationEDRowMapper : RowMapper<ApplicationED> {
    override fun map(rs: ResultSet, ctx: StatementContext): ApplicationED {
        return ApplicationED(
            rs.getLong("id"), rs.getString("name"), rs.getLong("version"),
            rs.getLong("createdMs"), rs.getLong("modifiedMs")
        )
    }
}

private class HostEDRowMapper : RowMapper<HostED> {
    override fun map(rs: ResultSet, ctx: StatementContext?): HostED {
        return HostED(rs.getLong("id"), rs.getString("name"), rs.getLong("createdMs"), rs.getLong("modifiedMs"))
    }
}

private class PropertyItemEDMapper : RowMapper<PropertyItemED> {
    override fun map(rs: ResultSet, ctx: StatementContext): PropertyItemED {
        return PropertyItemED(
            rs.getLong("id"), rs.getString("name"), rs.getString("value"), rs.getLong("hostId"),
            rs.getLong("appId"), rs.getLong("version"), rs.getBoolean("deleted"), rs.getLong("createdMs"),
            rs.getLong("modifiedMs")
        )
    }
}
