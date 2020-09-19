package com.configset.server.db.memory

import com.configset.server.db.AbstractConfigurationDaoTest
import com.configset.server.db.ConfigurationDao

class InMemoryConfigurationDaoTest : AbstractConfigurationDaoTest() {
    override fun getDao(): ConfigurationDao {
        return InMemoryConfigurationDao()
    }
}