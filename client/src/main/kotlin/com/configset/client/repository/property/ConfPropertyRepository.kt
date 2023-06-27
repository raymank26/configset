package com.configset.client.repository.property

import com.configset.client.ConfProperty
import com.configset.client.ConfigurationSnapshot
import com.configset.client.PropertyItem
import com.configset.client.repository.ConfigurationRepository

class ConfPropertyRepository(private val confProperty: ConfProperty<List<PropertyItem>>) : ConfigurationRepository {

    override fun start() {
    }

    override fun subscribeToProperties(appName: String): ConfigurationSnapshot {
        val snapshot = ConfigurationSnapshot(confProperty.getValue())
        confProperty.subscribe {
            snapshot.update(it)
        }
        return snapshot
    }

    override fun stop() {
    }
}