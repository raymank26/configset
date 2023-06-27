package com.configset.client.repository.local

import com.configset.client.ConfigurationSnapshot
import com.configset.client.ConfigurationTransport
import com.configset.client.PropertyItem
import com.configset.client.repository.ConfigurationRepository
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import java.io.Reader
import java.util.Properties

class LocalConfigurationRepository(
    private val localFormat: ConfigurationTransport.LocalFormat,
    private val readerProvider: () -> Reader,
) : ConfigurationRepository {

    private lateinit var properties: Map<String, List<PropertyItem>>

    override fun start() {
        properties = when (localFormat) {
            ConfigurationTransport.LocalFormat.TOML -> parseToml()
            ConfigurationTransport.LocalFormat.PROPERTIES -> parseProperties()
            else -> error("Unknown format $localFormat")
        }
    }

    private fun parseToml(): Map<String, List<PropertyItem>> {
        val tomlMapper = TomlMapper()
        return readerProvider.invoke().use { reader ->
            val tree = tomlMapper.readTree(reader) as ObjectNode
            tree.fields().asSequence().map { (appName, value) ->
                appName to parseValues(appName, value as ObjectNode)
            }.toMap()
        }
    }

    private fun parseValues(appName: String, appConfig: ObjectNode): List<PropertyItem> {
        return appConfig.fields().asSequence().map { (key, value) ->
            require(value is TextNode) { "Value for key = $key should contain text value" }
            PropertyItem(appName, key, 1L, value.textValue())
        }.toList()
    }

    private fun parseProperties(): Map<String, List<PropertyItem>> {
        return readerProvider.invoke().use { reader ->
            val properties = Properties()
            properties.load(reader)

            val config = mutableMapOf<String, List<PropertyItem>>()
            properties.forEach { key, value ->
                val (appName, propName) = (key as String).split(".", limit = 2)
                config.merge(appName, listOf(PropertyItem(appName, propName, 1L, value as String))) { a, b -> a + b }
            }
            config
        }
    }

    override fun subscribeToProperties(appName: String): ConfigurationSnapshot {
        return ConfigurationSnapshot(properties[appName] ?: emptyList())
    }

    override fun stop() = Unit
}
