package com.letsconfig

import com.letsconfig.db.ConfigurationDao

class InMemoryConfigurationDao : ConfigurationDao {

    override fun listApplications(): List<String> {
        TODO("Not yet implemented")
    }

    override fun createApplication(appName: String): CreateApplicationResult {
        TODO("Not yet implemented")
    }

    override fun createHost(hostName: String): HostCreateResult {
        TODO("Not yet implemented")
    }

    override fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: String): PropertyCreateResult {
        TODO("Not yet implemented")
    }

    override fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult {
        TODO("Not yet implemented")
    }

    override fun getConfigurationSnapshot(): Map<String, List<PropertyItem>> {
        TODO("Not yet implemented")
    }
}