package com.configset.client.repository.rewrite

import com.configset.client.ChangingObservable
import com.configset.client.Observable
import com.configset.client.PropertyItem
import com.configset.client.repository.ConfigApplication
import com.configset.client.repository.ConfigurationRepository

class RewriteConfigurationRepository(
    private val configurationRepository: ConfigurationRepository
) : ConfigurationRepository {

    private var snapshot: MutableMap<String, Snapshot> = mutableMapOf()

    override fun start() {
    }

    @Synchronized
    override fun subscribeToProperties(appName: String): ConfigApplication {
        val res = configurationRepository.subscribeToProperties(appName)
        snapshot[appName] = Snapshot(res.initial, res.observable)
        res.observable.onSubscribe { updates ->
            updateSnapshot(appName, updates)
        }
        return res
    }

    @Synchronized
    fun updateProperty(
        appName: String,
        name: String,
        value: String?
    ) {
        if (name !in snapshot) {
            snapshot[name] = Snapshot(emptyList(), ChangingObservable())
        }
        val appSnapshot = snapshot[appName]!!

        val newSnapshot = mutableListOf<PropertyItem>()

        var added = false
        for (propertyItem in appSnapshot.conf) {
            if (propertyItem.name == name) {
                added = true
                newSnapshot.add(PropertyItem(appName, name, propertyItem.version, value))
            } else {
                newSnapshot.add(propertyItem)
            }
        }
        if (!added) {
            newSnapshot.add(PropertyItem(appName, name, 1L, value))
        }
        appSnapshot.observable.push(newSnapshot)
        snapshot[appName] = Snapshot(newSnapshot, appSnapshot.observable)
    }

    @Synchronized
    private fun updateSnapshot(appName: String, newSnapshot: List<PropertyItem>) {
        snapshot[appName] = Snapshot(newSnapshot, snapshot[appName]!!.observable)
    }

    @Synchronized
    override fun stop() {
        configurationRepository.stop()
    }
}

private class Snapshot(val conf: List<PropertyItem>, val observable: Observable<List<PropertyItem>>)