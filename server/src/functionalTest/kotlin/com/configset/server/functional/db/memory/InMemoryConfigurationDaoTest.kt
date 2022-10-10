package com.configset.server.functional.db.memory

import com.configset.server.db.ConfigurationDao
import com.configset.server.db.DbHandleFactory
import com.configset.server.db.memory.InMemoryConfigurationDao
import com.configset.server.db.memory.InMemoryDbHandleFactory
import com.configset.server.functional.db.AbstractConfigurationDaoTest

class InMemoryConfigurationDaoTest : AbstractConfigurationDaoTest() {

    override fun getDao(): ConfigurationDao {
        return InMemoryConfigurationDao()
    }

    override fun getDbHandleFactory(): DbHandleFactory {
        return InMemoryDbHandleFactory
    }
}