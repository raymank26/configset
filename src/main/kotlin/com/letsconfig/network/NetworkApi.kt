package com.letsconfig.network

import com.letsconfig.model.PropertiesObserver

interface NetworkApi {
    fun createApplication(appName: String): CreateApplicationResult
    fun createHost(hostName: String): HostCreateResult
    fun updateProperty(appName: String, hostName: String, propertyName: String, value: String): PropertyCreateResult
    fun deleteProperty(appName: String, hostName: String, propertyName: String): DeletePropertyResult
    fun subscribe(observer: PropertiesObserver)
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
