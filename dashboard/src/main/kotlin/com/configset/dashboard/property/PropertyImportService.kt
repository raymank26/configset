package com.configset.dashboard.property

import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.util.RequestIdProducer
import com.configset.sdk.auth.UserInfo
import com.configset.sdk.extension.createLoggerStatic
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

private val xmlMapper = XmlMapper()
private val LOG = createLoggerStatic<PropertyImportService>()

class PropertyImportService(
    private val serverApiGateway: ServerApiGateway,
    private val requestIdProducer: RequestIdProducer,
) {

    fun import(
        requestId: String,
        appName: String,
        properties: String,
        userInfo: UserInfo,
    ) {
        val root = try {
            xmlMapper.readValue(properties, PropertiesXml::class.java)
        } catch (e: Exception) {
            LOG.warn("Illegal format of input data", e)
            throw ImportErrorType.ILLEGAL_FORMAT.throwException()
        }
        var curRequestId = requestId
        val hosts = serverApiGateway.listHosts(userInfo).toSet()
        for (property in root.properties) {
            if (!hosts.contains(property.host)) {
                serverApiGateway.createHost(curRequestId, property.host, userInfo)
                curRequestId = requestIdProducer.nextRequestId(curRequestId)
            }

            val alreadyCreatedProperty = serverApiGateway.readProperty(
                appName,
                property.host,
                property.name,
                userInfo
            )
            val version = alreadyCreatedProperty?.version

            serverApiGateway.updateProperty(
                curRequestId,
                appName,
                property.host,
                property.name,
                property.value,
                version,
                userInfo
            )
        }
    }
}

@JacksonXmlRootElement(localName = "properties")
class PropertiesXml {

    @JacksonXmlProperty(localName = "property")
    @JacksonXmlElementWrapper(useWrapping = false)
    var properties: MutableList<PropertyXml> = ArrayList()

}

class PropertyXml {

    @JsonProperty("host")
    lateinit var host: String

    @JsonProperty("name")
    lateinit var name: String

    @JsonProperty("value")
    lateinit var value: String
}
