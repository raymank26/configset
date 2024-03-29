package com.configset.server.db.postgres

import com.configset.common.client.ApplicationId
import com.configset.server.ApplicationED
import com.configset.server.CreateApplicationResult
import com.configset.server.DeleteApplicationResult
import com.configset.server.DeletePropertyResult
import com.configset.server.HostCreateResult
import com.configset.server.HostED
import com.configset.server.PropertyCreateResult
import com.configset.server.SearchPropertyRequest
import com.configset.server.UpdateApplicationResult
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.PropertyItemED
import com.configset.server.db.common.DbHandle
import com.configset.server.db.postgres.PropertyItemEDMapper.Companion.PROPERTY_ED_SELECT_EXP
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.SqlStatements
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.sql.ResultSet

class PostgreSqlConfigurationDao(dbi: Jdbi) : ConfigurationDao {

    init {
        dbi.registerRowMapper(ApplicationEDRowMapper())
        dbi.registerRowMapper(HostEDRowMapper())
        dbi.registerRowMapper(PropertyItemEDMapper())
        dbi.registerRowMapper(PropertyItemEDMapper())
        dbi.getConfig(SqlStatements::class.java).isUnusedBindingAllowed = true
    }

    override fun initialize() {
    }

    override fun listApplications(handle: DbHandle): List<ApplicationED> {
        return handle.withApi(JdbiAccess::class.java) {
            it.listApplications()
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

    override fun deleteApplication(handle: DbHandle, applicationName: String): DeleteApplicationResult {
        return handle.inTransaction {
            val res = it.createUpdate("update ConfigurationApplication set deleted = true where name = :name")
                .bind("name", applicationName)
                .execute()
            if (res > 0) DeleteApplicationResult.OK else DeleteApplicationResult.ApplicationNotFound
        }
    }

    override fun updateApplication(
        handle: DbHandle,
        id: ApplicationId,
        applicationName: String
    ): UpdateApplicationResult {
        return handle.inTransaction {
            val res = it.createUpdate(
                "update ConfigurationApplication set name = :name where id = :id and deleted = false"
            )
                .bind("id", id.id)
                .bind("name", applicationName)
                .execute()
            if (res > 0) UpdateApplicationResult.OK else UpdateApplicationResult.ApplicationNotFound
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

    override fun listHosts(handle: DbHandle): List<HostED> {
        return handle.withApi(JdbiAccess::class.java) { access ->
            access.listHosts()
        }
    }

    override fun readProperty(
        handle: DbHandle,
        hostName: String,
        propertyName: String,
        applicationName: String
    ): PropertyItemED? {
        return handle.inTransaction {
            it.createQuery(
                """select $PROPERTY_ED_SELECT_EXP from ConfigurationProperty cp
                | join ConfigurationApplication ca on ca.id = cp.appId
                | join ConfigurationHost ch on ch.id = cp.hostId
                | where ch.name = :hostName and ca.name = :appName and cp.name = :propertyName
                """.trimMargin()
            )
                .bind("hostName", hostName)
                .bind("appName", applicationName)
                .bind("propertyName", propertyName)
                .mapTo(PropertyItemED::class.java)
                .findFirst()
                .orElse(null)
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
        return processMutable(handle, fun(access: JdbiAccess): PropertyCreateResult {
            val app = access.getApplicationByName(appName)
                ?: return PropertyCreateResult.ApplicationNotFound
            val host = access.getHostByName(hostName)
                ?: return PropertyCreateResult.HostNotFound
            val property = readProperty(handle, hostName, propertyName, appName)

            val ct = System.currentTimeMillis()
            if (property == null && version == null) {
                access.insertProperty(propertyName, value, app.lastVersion + 1, app.id.id, host.id!!, ct)
                access.incrementAppVersion(app.id.id)
                return PropertyCreateResult.OK
            } else if (property != null) {
                if (!property.deleted && property.version != version) {
                    return PropertyCreateResult.UpdateConflict
                } else {
                    access.updateProperty(
                        id = property.id!!,
                        value = value,
                        version = app.lastVersion + 1,
                        deleted = false,
                        modifiedMs = ct,
                        appId = app.id.id,
                        name = propertyName,
                        hostId = host.id!!
                    )
                    access.incrementAppVersion(app.id.id)
                    return PropertyCreateResult.OK
                }
            } else {
                return PropertyCreateResult.UpdateConflict
            }
        })
    }

    override fun deleteProperty(
        handle: DbHandle,
        appName: String,
        hostName: String,
        propertyName: String,
        version: Long,
    ): DeletePropertyResult {
        return processMutable(handle, fun(access: JdbiAccess): DeletePropertyResult {
            val app = access.getApplicationByName(appName)
                ?: return DeletePropertyResult.PropertyNotFound
            val host = access.getHostByName(hostName)
                ?: return DeletePropertyResult.PropertyNotFound
            val property = readProperty(handle, host.name, propertyName, app.name)
                ?: return DeletePropertyResult.PropertyNotFound
            if (property.version != version) {
                return DeletePropertyResult.DeleteConflict
            }
            access.markPropertyAsDeleted(property.id!!, app.lastVersion + 1)
            access.incrementAppVersion(app.id.id)
            return DeletePropertyResult.OK
        })
    }

    override fun getConfigurationSnapshotList(handle: DbHandle): List<PropertyItemED> {
        return handle.inTransaction {
            it.createQuery(
                """select $PROPERTY_ED_SELECT_EXP from ConfigurationProperty cp
                | join ConfigurationApplication ca on ca.id = cp.appId
                | join ConfigurationHost ch on ch.id = cp.hostId
                | where ca.deleted = false
                """.trimMargin()
            )
                .mapTo(PropertyItemED::class.java)
                .list()
        }
    }

    override fun searchProperties(
        handle: DbHandle,
        searchPropertyRequest: SearchPropertyRequest
    ): List<PropertyItemED> {
        val conditionList = mutableListOf<String>()
        conditionList.add("true")
        if (searchPropertyRequest.propertyNameQuery != null) {
            conditionList.add("cp.name ilike :propertyName")
        }
        if (searchPropertyRequest.propertyValueQuery != null) {
            conditionList.add("cp.value ilike :propertyValue")
        }
        if (searchPropertyRequest.applicationName != null) {
            conditionList.add("ca.name ilike :appName")
        }
        if (searchPropertyRequest.hostNameQuery != null) {
            conditionList.add("ch.name ilike :hostName")
        }
        return handle.inTransaction {
            it.createQuery(
                """select $PROPERTY_ED_SELECT_EXP from ConfigurationProperty cp
                | join ConfigurationApplication ca on ca.id = cp.appId
                | join ConfigurationHost ch on ch.id = cp.hostId
                | where ${conditionList.joinToString(" and ")} and not cp.deleted
                """.trimMargin()
            )
                .bind("propertyName", likeWildcard(searchPropertyRequest.propertyNameQuery))
                .bind("propertyValue", likeWildcard(searchPropertyRequest.propertyValueQuery))
                .bind("appName", likeWildcard(searchPropertyRequest.applicationName))
                .bind("hostName", likeWildcard(searchPropertyRequest.hostNameQuery))
                .mapTo(PropertyItemED::class.java)
                .list()
        }
    }

    private fun likeWildcard(propertyNameQuery: String?): String? {
        return propertyNameQuery?.let {
            return "%$it%"
        }
    }

    override fun listProperties(handle: DbHandle, applicationName: String): List<String> {
        return handle.inTransaction {
            it.createQuery(
                """select distinct cp.name from ConfigurationProperty cp
                | join ConfigurationApplication ca on ca.id = cp.appId
                | where ca.name = :appName and cp.deleted = false and ca.deleted = false
                """.trimMargin()
            )
                .bind("appName", applicationName)
                .mapTo(String::class.java)
                .list()
        }
    }

    private fun <K> processMutable(handle: DbHandle, callback: (access: JdbiAccess) -> K): K {
        return handle.withApi(JdbiAccess::class.java) {
            callback(it)
        }
    }
}

private interface JdbiAccess {

    @SqlQuery("select * from ConfigurationApplication where deleted = false")
    fun listApplications(): List<ApplicationED>

    @SqlQuery("select * from ConfigurationApplication where name = :name and deleted = false")
    fun getApplicationByName(@Bind("name") name: String): ApplicationED?

    @SqlUpdate("insert into ConfigurationApplication (name, version, deleted, createdMs, modifiedMs) values (:name, 0, false, :createdMs, :createdMs)")
    fun insertApplication(@Bind("name") name: String, @Bind("createdMs") createdMs: Long)

    @SqlUpdate("update ConfigurationApplication set version = version + 1 WHERE id = :id")
    fun incrementAppVersion(@Bind("id") id: Long)

    @SqlUpdate("insert into ConfigurationHost (name, createdMs, modifiedMs) values (:name, :createdMs, :createdMs)")
    fun insertHost(@Bind("name") name: String, @Bind("createdMs") createdMs: Long)

    @SqlQuery("select * from ConfigurationHost where name = :name")
    fun getHostByName(@Bind("name") name: String): HostED?

    @SqlQuery("select * from ConfigurationHost")
    fun listHosts(): List<HostED>

    @SqlUpdate(
        "insert into ConfigurationProperty (name, value, version, appId, hostId, deleted, createdMs, modifiedMs)" +
            " values (:name, :value, :version, :appId, :hostId, false, :createdMs, :createdMs)"
    )
    fun insertProperty(
        @Bind("name") name: String,
        @Bind("value") value: String,
        @Bind("version") version: Long,
        @Bind("appId") appId: Long,
        @Bind("hostId") hostId: Long,
        @Bind("createdMs") modifiedMs: Long,
    )

    @SqlUpdate(
        "update ConfigurationProperty set value = :value, version = :version, deleted = :deleted, modifiedMs = :modifiedMs " +
                "where appId = :appId and name = :name and hostId = :hostId"
    )
    fun updateProperty(
        @Bind("id") id: Long,
        @Bind("value") value: String,
        @Bind("version") version: Long,
        @Bind("deleted") deleted: Boolean,
        @Bind("modifiedMs") modifiedMs: Long,
        @Bind("appId") appId: Long,
        @Bind("name") name: String,
        @Bind("hostId") hostId: Long,
    )

    @SqlUpdate("update ConfigurationProperty SET deleted = true, version = :version where id = :id")
    fun markPropertyAsDeleted(@Bind("id") id: Long, @Bind("version") version: Long)
}

private class ApplicationEDRowMapper : RowMapper<ApplicationED> {
    override fun map(rs: ResultSet, ctx: StatementContext): ApplicationED {
        return ApplicationED(
            id = ApplicationId(rs.getLong("id")),
            name = rs.getString("name"),
            lastVersion = rs.getLong("version"),
            createdMs = rs.getLong("createdMs"),
            modifiedMs = rs.getLong("modifiedMs")
        )
    }
}

private class HostEDRowMapper : RowMapper<HostED> {
    override fun map(rs: ResultSet, ctx: StatementContext?): HostED {
        return HostED(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            createdMs = rs.getLong("createdMs"),
            modifiedMs = rs.getLong("modifiedMs")
        )
    }
}

private class PropertyItemEDMapper : RowMapper<PropertyItemED> {

    companion object {
        val PROPERTY_ED_SELECT_EXP = ResultSetPrefixFetcherBuilder.buildSelectExp(listOf("ca", "ch", "cp"))
    }

    override fun map(rs: ResultSet, ctx: StatementContext): PropertyItemED {
        val rsf = ResultSetPrefixFetcher.getFetcher(this::class.java, rs)
        return PropertyItemED(
            id = rsf.getLong("cp.id"),
            name = rsf.getString("cp.name"),
            value = rsf.getString("cp.value"),
            hostName = rsf.getString("ch.name"),
            applicationName = rsf.getString("ca.name"),
            version = rsf.getLong("cp.version"),
            deleted = rsf.getBoolean("cp.deleted"),
            createdMs = rsf.getLong("cp.createdms"),
            modifiedMs = rsf.getLong("cp.modifiedms")
        )
    }
}
