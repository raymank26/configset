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
import com.configset.server.TableMetaED
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.common.PersistResult
import com.configset.server.db.common.containsLowerCase
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.core.transaction.TransactionIsolationLevel
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.postgresql.util.PSQLException
import java.sql.ResultSet
import java.util.stream.Collectors


class PostgreSqlConfigurationDao(private val dbi: Jdbi) : ConfigurationDao {

    private val logger = createLogger()

    init {
        dbi.registerRowMapper(ApplicationEDRowMapper())
        dbi.registerRowMapper(HostEDRowMapper())
        dbi.registerRowMapper(PropertyItemEDMapper())
        dbi.registerRowMapper(PropertyItemEDMapper())
        dbi.registerRowMapper(TableMetaEDMapper())
    }

    override fun initialize() {
        val lastVersion = getLastVersion()
        val resource = javaClass.classLoader.getResource("migration/list.txt")
        require(resource != null)
        val migrations: List<Migration> = resource
            .openStream()
            .use { stream ->
                stream.bufferedReader()
                    .lines()
                    .map { line ->
                        val version = line.split("__")[0]
                        Migration(version.toLong(), line)
                    }
                    .filter { it.version > lastVersion }
                    .sorted(compareBy { it.version })
                    .collect(Collectors.toList())
            }
        logger.info("Found last table version = $lastVersion and new migrations ${migrations.size})")
        dbi.useHandle<Exception> { handle ->
            for (migrationName in migrations) {
                val migrationResource = javaClass.classLoader.getResource("migration/${migrationName.resourceName}")
                require(migrationResource != null)
                val content = migrationResource.openStream().bufferedReader().readText()
                handle.createUpdate(content).execute()
                logger.info("Applied migration $migrationName")
            }
        }
        if (migrations.isNotEmpty()) {
            val lastApplied = migrations.last()
            if (lastVersion == 0L) {
                dbi.withExtension { access ->
                    access.insertTableMeta(lastApplied.version)
                }
            } else {
                dbi.withExtension { access ->
                    access.updateTableMetaVersion(lastApplied.version)
                }
            }
            val lastSavedVersion = getLastVersion()
            require(lastSavedVersion == lastApplied.version)
        }
        logger.info("Initialization completed")
    }

    private fun getLastVersion(): Long {
        try {
            val tableMeta = dbi.withExtension { access ->
                access.tableMeta()
            }
            return tableMeta?.version ?: 0
        } catch (e: Exception) {
            if (e.cause is PSQLException && (e.cause as PSQLException).sqlState == "42P01") {
                return 0
            }
            throw e
        }
    }

    override fun listApplications(): List<ApplicationED> {
        return dbi.withExtension { access ->
            access.listApplications()
        }
    }

    override fun createApplication(requestId: String, appName: String): CreateApplicationResult {
        return processMutable(requestId, CreateApplicationResult.OK) { _, access ->
            val createdApp = access.getApplicationByName(appName)
            if (createdApp != null) {
                PersistResult(false, CreateApplicationResult.ApplicationAlreadyExists)
            } else {
                access.insertApplication(appName, System.currentTimeMillis())
                PersistResult(true, CreateApplicationResult.OK)
            }
        }
    }

    override fun createHost(requestId: String, hostName: String): HostCreateResult {
        return processMutable(requestId, HostCreateResult.OK) { _, access ->
            val host = access.getHostByName(hostName)
            if (host != null) {
                PersistResult(false, HostCreateResult.HostAlreadyExists)
            } else {
                access.insertHost(hostName, System.currentTimeMillis())
                PersistResult(true, HostCreateResult.OK)
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
        requestId: String,
        appName: String,
        hostName: String,
        propertyName: String,
        value: String,
        version: Long?
    ): PropertyCreateResult {
        return processMutable(requestId, PropertyCreateResult.OK) cb@{ _, access ->
            val app = access.getApplicationByName(appName)
                ?: return@cb PersistResult(false, PropertyCreateResult.ApplicationNotFound)
            val host = access.getHostByName(hostName)
                ?: return@cb PersistResult(false, PropertyCreateResult.HostNotFound)
            val property = access.getProperty(propertyName, host.id!!, app.id!!)

            val ct = System.currentTimeMillis()
            if (property == null && version == null) {
                access.insertProperty(propertyName, value, app.lastVersion + 1, app.id, host.id, ct)
                access.incrementAppVersion(app.id)
                return@cb PersistResult(true, PropertyCreateResult.OK)
            } else if (property != null) {
                if (!property.deleted && property.version != version) {
                    return@cb PersistResult(false, PropertyCreateResult.UpdateConflict)
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
                    return@cb PersistResult(true, PropertyCreateResult.OK)
                }
            } else {
                return@cb PersistResult(false, PropertyCreateResult.UpdateConflict)
            }
        }
    }

    override fun deleteProperty(
        requestId: String,
        appName: String,
        hostName: String,
        propertyName: String,
        version: Long
    ): DeletePropertyResult {
        return processMutable(requestId, DeletePropertyResult.OK) cb@{ _, access ->
            val app = access.getApplicationByName(appName)
                ?: return@cb PersistResult(false, DeletePropertyResult.PropertyNotFound)
            val host = access.getHostByName(hostName)
                ?: return@cb PersistResult(false, DeletePropertyResult.PropertyNotFound)
            val property = access.getProperty(propertyName, host.id!!, app.id!!)
                ?: return@cb PersistResult(false, DeletePropertyResult.PropertyNotFound)
            if (property.version != version) {
                return@cb PersistResult(false, DeletePropertyResult.DeleteConflict)
            }
            access.markPropertyAsDeleted(property.id!!, app.lastVersion + 1)
            access.incrementAppVersion(app.id)
            PersistResult(true, DeletePropertyResult.OK)
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

    private fun <T> processMutable(
        requestId: String,
        default: T,
        callback: (handle: Handle, access: JdbiAccess) -> PersistResult<T>
    ): T {
        return dbi.inTransaction<T, Exception>(TransactionIsolationLevel.SERIALIZABLE) { handle: Handle ->
            val access = handle.attach(JdbiAccess::class.java)
            val alreadyProcessed = access.getRequestIdCount(requestId) > 0
            if (alreadyProcessed) {
                return@inTransaction default
            }
            val res = callback.invoke(handle, access)
            if (res.persistRequestId) {
                access.insertRequestId(requestId, System.currentTimeMillis())
            }
            res.res
        }
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

    @SqlQuery("select * from TableMeta")
    fun tableMeta(): TableMetaED?

    @SqlUpdate("insert into TableMeta (version) values (:version)")
    fun insertTableMeta(@Bind("version") version: Long)

    @SqlUpdate("update TableMeta set version = :version")
    fun updateTableMetaVersion(@Bind("version") version: Long)

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

    @SqlUpdate("insert into RequestId (requestId, createdMs) values (:requestId, :createdMs)")
    fun insertRequestId(@Bind("requestId") requestId: String, @Bind("createdMs") createdMs: Long)

    @SqlQuery("select count(*) from RequestId where requestId = :requestId")
    fun getRequestIdCount(@Bind("requestId") requestId: String): Int

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

private class TableMetaEDMapper : RowMapper<TableMetaED> {
    override fun map(rs: ResultSet, ctx: StatementContext): TableMetaED {
        return TableMetaED(rs.getLong("version"))
    }
}

private fun <T> Jdbi.withExtension(func: (access: JdbiAccess) -> T): T {
    return this.withExtension<T, JdbiAccess, Exception>(JdbiAccess::class.java) { access ->
        func.invoke(access)
    }
}

private data class Migration(val version: Long, val resourceName: String)
