package com.letsconfig.dashboard.property

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.letsconfig.dashboard.PropertyCreateResult
import com.letsconfig.dashboard.ServerApiGateway
import com.letsconfig.dashboard.util.RequestIdProducer
import com.letsconfig.sdk.extension.createLoggerStatic

private val xmlMapper = XmlMapper()
private val LOG = createLoggerStatic<PropertyImportService>()

class PropertyImportService(
        private val serverApiGateway: ServerApiGateway,
        private val requestIdProducer: RequestIdProducer
) {

    fun import(requestId: String, appName: String, properties: String): PropertiesImport {
        try {
            val root = xmlMapper.readValue(properties, PropertiesXml::class.java)
            var curRequestId = requestId
            val hosts = serverApiGateway.listHosts().toSet()
            for (property in root.properties) {
                if (!hosts.contains(property.host)) {
                    serverApiGateway.createHost(curRequestId, property.host)
                    curRequestId = requestIdProducer.nextRequestId(curRequestId)
                }

                var retired = false
                loop@ while (true) {
                    val alreadyCreatedProperty = serverApiGateway.readProperty(appName, property.host, property.name)
                    val version = alreadyCreatedProperty?.version
                    when (serverApiGateway.updateProperty(curRequestId, appName, property.host, property.name, property.value, version)) {
                        PropertyCreateResult.OK -> Unit
                        PropertyCreateResult.ApplicationNotFound -> return PropertiesImport.ApplicationNotFound
                        PropertyCreateResult.UpdateConflict -> if (retired) {
                            throw RuntimeException("Already retried, but conflict occurred")
                        } else {
                            curRequestId = requestIdProducer.nextRequestId(curRequestId)
                            retired = true
                            continue@loop
                        }
                    }
                    curRequestId = requestIdProducer.nextRequestId(curRequestId)
                    break
                }
            }
            return PropertiesImport.OK
        } catch (e: Exception) {
            LOG.info("Exception has occurred", e)
            return PropertiesImport.IllegalFormat
        }
    }
}

sealed class PropertiesImport {
    object ApplicationNotFound : PropertiesImport()
    object OK: PropertiesImport()
    object IllegalFormat: PropertiesImport()
}


@JacksonXmlRootElement(localName = "properties")
class PropertiesXml() {

    @JacksonXmlProperty(localName = "property")
    @JacksonXmlElementWrapper(useWrapping = false)
    var properties: MutableList<PropertyXml> = ArrayList()

}

class PropertyXml() {

    @JsonProperty("host")
    lateinit var host: String

    @JsonProperty("name")
    lateinit var name: String

    @JsonProperty("value")
    lateinit var value: String
}