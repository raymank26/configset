package com.configset.server.db.postgres

import com.configset.server.db.RequestIdDao
import com.configset.server.db.common.DbHandle
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

class RequestIdSqlDao : RequestIdDao {
    override fun exists(handle: DbHandle, requestId: String): Boolean {
        return handle.getApi(RequestIdSqlDaoDbi::class.java).getRequestIdCount(requestId) > 0
    }

    override fun persist(handle: DbHandle, requestId: String) {
        return handle.getApi(RequestIdSqlDaoDbi::class.java)
            .insertRequestId(requestId, System.currentTimeMillis())
    }
}


private interface RequestIdSqlDaoDbi {
    @SqlUpdate("insert into RequestId (requestId, createdMs) values (:requestId, :createdMs)")
    fun insertRequestId(@Bind("requestId") requestId: String, @Bind("createdMs") createdMs: Long)

    @SqlQuery("select count(*) from RequestId where requestId = :requestId")
    fun getRequestIdCount(@Bind("requestId") requestId: String): Int

}
