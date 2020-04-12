package com.letsconfig.db.memory

import com.letsconfig.db.AbstractConfigurationDaoTest
import com.letsconfig.db.ConfigurationDao

class InMemoryConfigurationDaoTest : AbstractConfigurationDaoTest() {
    override fun getDao(): ConfigurationDao {
        return InMemoryConfigurationDao()
    }
}