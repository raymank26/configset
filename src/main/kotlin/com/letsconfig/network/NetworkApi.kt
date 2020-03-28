package com.letsconfig.network

interface NetworkApi {
    fun listApplications(): List<String>
    fun createApplication(appName: String): CreateApplicationResult
    fun createHost(hostName: String): HostCreateResult
    fun updateProperty(appName: String, hostName: String, propertyName: String, value: String): PropertyCreateResult
    fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult
    fun subscribeApplication(subscriberId: String, hostName: String, applicationName: String): List<PropertyItem.Updated>
    fun watchChanges(subscriber: WatchSubscriber)
    fun unsubscribe(subscriberId: String)
}

interface WatchSubscriber {
    fun getId(): String
    fun pushChanges(change: PropertyItem)
}

sealed class PropertyItem {
    data class Updated(val applicationName: String, val name: String, val value: String, val version: Long) : PropertyItem()
    data class Deleted(val applicationName: String, val name: String, val version: Long) : PropertyItem()
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
}

sealed class DeletePropertyResult {
    object OK : DeletePropertyResult()
    object HostNotFound : DeletePropertyResult()
    object ApplicationNotFound : DeletePropertyResult()
}
