package com.configset.client

import java.util.concurrent.ConcurrentHashMap

class ConfigurationSnapshot(values: List<PropertyItem>) {

    private val snapshot = ConcurrentHashMap<SnapshotKey, PropertyItem>()
    private val subscribers = ConcurrentHashMap<SnapshotKey, List<Subscriber<String?>>>()

    init {
        snapshot.putAll(values.associateBy { SnapshotKey(it.name) })
    }

    fun update(values: List<PropertyItem>) {
        for (value in values) {
            val snapshotKey = SnapshotKey(value.name)
            snapshot[snapshotKey] = value
            (subscribers[snapshotKey] ?: emptyList()).forEach { subscriber ->
                subscriber.process(value.value)
            }
        }
    }

    fun get(propertyName: String): PropertyItem? {
        return snapshot[SnapshotKey(propertyName)]
    }

    fun subscribe(propertyName: String, subscriber: Subscriber<String?>) {
        subscribers.compute(SnapshotKey(propertyName)) { _, prev ->
            (prev ?: emptyList()) + subscriber
        }
    }
}

private data class SnapshotKey(val propertyKey: String)



