package com.letsconfig

interface ConfigurationService {
    fun listApplications(): List<String>
    fun createApplication(appName: String): CreateApplicationResult
    fun createHost(hostName: String): HostCreateResult
    fun updateProperty(appName: String, hostName: String, propertyName: String, value: String, version: Long?): PropertyCreateResult
    fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult
    fun subscribeApplication(subscriberId: String, defaultApplicationName: String, hostName: String, applicationName: String,
                             lastKnownVersion: Long?): List<PropertyItem>

    fun watchChanges(subscriber: WatchSubscriber)
    fun unsubscribe(subscriberId: String)
}

interface WatchSubscriber {
    fun getId(): String
    fun pushChanges(change: PropertyItem)
}

sealed class PropertyItem {

    abstract val applicationName: String
    abstract val name: String
    abstract val version: Long
    abstract val hostName: String

    data class Updated(
            override val applicationName: String,
            override val name: String,
            override val hostName: String,
            override val version: Long,
            val value: String
    ) : PropertyItem()

    data class Deleted(
            override val applicationName: String,
            override val name: String,
            override val hostName: String,
            override val version: Long
    ) : PropertyItem()
}

sealed class CreateApplicationResult {
    object OK : CreateApplicationResult()
    object ApplicationAlreadyExists : CreateApplicationResult()
}

sealed class HostCreateResult {
    object OK : HostCreateResult()
    object HostAlreadyExists : HostCreateResult()
}

sealed class PropertyCreateResult {
    object OK : PropertyCreateResult()
    object HostNotFound : PropertyCreateResult()
    object ApplicationNotFound : PropertyCreateResult()
    object UpdateConflict : PropertyCreateResult()
}

sealed class DeletePropertyResult {
    object OK : DeletePropertyResult()
    object PropertyNotFound : DeletePropertyResult()
}
