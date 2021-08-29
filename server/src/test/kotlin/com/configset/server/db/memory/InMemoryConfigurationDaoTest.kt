package com.configset.server.db.memory

import com.configset.server.db.AbstractConfigurationDaoTest
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.DbHandleFactory

class InMemoryConfigurationDaoTest : AbstractConfigurationDaoTest() {
    override fun getDao(): ConfigurationDao {
        return InMemoryConfigurationDao()
    }

    override fun getDbHandleFactory(): DbHandleFactory {
        return InMemoryDbHandleFactory
    }
}