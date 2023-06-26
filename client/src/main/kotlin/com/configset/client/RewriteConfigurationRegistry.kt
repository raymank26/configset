package com.configset.client

import com.configset.client.repository.rewrite.RewriteConfigurationRepository

class RewriteConfigurationRegistry(
    private val rewriteConfigurationRepository: RewriteConfigurationRepository
) : ConfigurationRegistry(rewriteConfigurationRepository) {

    fun updateProperty(
        appName: String,
        name: String,
        value: String?
    ) {
        rewriteConfigurationRepository.updateProperty(appName, name, value)
    }
}