package com.configset.dashboard.property

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.configset.dashboard.ConfigurationUpdateError
import com.configset.dashboard.ServerApiGateway
import com.configset.dashboard.util.RequestIdProducer
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
        accessToken: String,
    ): Either<ImportError, Unit> {
        val root = try {
            xmlMapper.readValue(properties, PropertiesXml::class.java)
        } catch (e: Exception) {
            LOG.warn("Illegal format of input data", e)
            return ImportError.ILLEGAL_FORMAT.left()
        }
        var curRequestId = requestId
        val hosts = serverApiGateway.listHosts(accessToken).toSet()
        for (property in root.properties) {
            if (!hosts.contains(property.host)) {
                serverApiGateway.createHost(curRequestId, property.host, accessToken)
                curRequestId = requestIdProducer.nextRequestId(curRequestId)
            }

            val alreadyCreatedProperty = serverApiGateway.readProperty(
                appName,
                property.host,
                property.name,
                accessToken
            )
            val version = alreadyCreatedProperty?.version

            val updatePropertyResult = serverApiGateway.updateProperty(
                curRequestId,
                appName,
                property.host,
                property.name,
                property.value,
                version,
                accessToken
            )
            val updateResult = updatePropertyResult.mapLeft {
                when (it) {
                    ConfigurationUpdateError.CONFLICT -> ImportError.ILLEGAL_FORMAT
                    ConfigurationUpdateError.APPLICATION_NOT_FOUND -> ImportError.APPLICATION_NOT_FOUND
                    ConfigurationUpdateError.HOST_NOT_FOUND -> ImportError.HOST_NOT_FOUND
                    else -> error("Unknown error $it")
                }
            }
            if (updateResult.isLeft()) {
                return updateResult
            }
        }
        return Unit.right()
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

enum class ImportError {
    APPLICATION_NOT_FOUND,
    HOST_NOT_FOUND,
    ILLEGAL_FORMAT,
    PROPERTY_CONFLICT
}