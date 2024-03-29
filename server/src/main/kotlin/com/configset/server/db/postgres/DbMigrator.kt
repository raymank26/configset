package com.configset.server.db.postgres

import com.configset.common.client.extension.createLoggerStatic
import com.configset.server.TableMetaED
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.postgresql.util.PSQLException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.sql.ResultSet
import java.util.stream.Collectors

private val logger = createLoggerStatic<DbMigrator>()

class DbMigrator(private val dbi: Jdbi) {

    init {
        dbi.registerRowMapper(TableMetaEDMapper())
    }

    fun migrate() {
        val lastVersion = getLastVersion()
        val migrationFolder = DbMigrator::class.java.getResource("/migration")
            ?: error("Migrations not found")

        val files = FileSystems.newFileSystem(migrationFolder.toURI(), emptyMap<String, Any>()).use { fs ->
            Files.walk(fs.getPath("/migration"))
                .use { filesStream ->
                    filesStream.filter(Files::isRegularFile)
                        .map { it.fileName.toString() }
                        .collect(Collectors.toList())
                }
        }

        val migrations = files
            .asSequence()
            .map { fileName ->
                val version = fileName.split("__").getOrNull(0)
                    ?: error("Cannot find '__' substring in filename = $fileName")
                Migration(version.toLong(), fileName)
            }
            .filter { it.version > lastVersion }
            .sortedBy { it.version }
            .toList()
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
                dbi.withExtension<Unit, TableMetaAccess> { access ->
                    access.insertTableMeta(lastApplied.version)
                }
            } else {
                dbi.withExtension<Unit, TableMetaAccess> { access ->
                    access.updateTableMetaVersion(lastApplied.version)
                }
            }
            val lastSavedVersion = getLastVersion()
            require(lastSavedVersion == lastApplied.version)
        }
        logger.info("Migration completed")
    }

    private fun getLastVersion(): Long {
        try {
            val tableMeta = dbi.withExtension<TableMetaED?, TableMetaAccess> { access ->
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
}

private interface TableMetaAccess {

    @SqlQuery("select * from TableMeta")
    fun tableMeta(): TableMetaED?

    @SqlUpdate("insert into TableMeta (version) values (:version)")
    fun insertTableMeta(@Bind("version") version: Long)

    @SqlUpdate("update TableMeta set version = :version")
    fun updateTableMetaVersion(@Bind("version") version: Long)
}

private data class Migration(val version: Long, val resourceName: String)

inline fun <T, reified K> Jdbi.withExtension(crossinline func: (access: K) -> T): T {
    return this.withExtension<T, K, Exception>(K::class.java) { access ->
        func.invoke(access)
    }
}

private class TableMetaEDMapper : RowMapper<TableMetaED> {
    override fun map(rs: ResultSet, ctx: StatementContext): TableMetaED {
        return TableMetaED(rs.getLong("version"))
    }
}
