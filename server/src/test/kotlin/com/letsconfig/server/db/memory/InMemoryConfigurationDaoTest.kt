package com.letsconfig.server.db.memory

import com.letsconfig.server.db.AbstractConfigurationDaoTest
import com.letsconfig.server.db.ConfigurationDao

class InMemoryConfigurationDaoTest : AbstractConfigurationDaoTest() {
    override fun getDao(): ConfigurationDao {
        return InMemoryConfigurationDao()
    }
}