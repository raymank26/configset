package com.configset.client.repository.local

import com.configset.client.ConfigurationSnapshot
import com.configset.client.ConfigurationSource
import com.configset.client.FileFormat
import com.configset.client.FileSourceType
import com.configset.client.PropertyItem
import com.configset.client.repository.ConfigurationRepository
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.StorageOptions
import java.io.Reader
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

class FileTransportRepository(
    private val configurationSource: ConfigurationSource.File,
) : ConfigurationRepository {

    private lateinit var properties: Map<String, List<PropertyItem>>

    override fun start() {
        properties = when (configurationSource.format) {
            FileFormat.TOML -> parseToml()
            FileFormat.PROPERTIES -> parseProperties()
        }
    }

    private fun parseToml(): Map<String, List<PropertyItem>> {
        val tomlMapper = TomlMapper()
        return openReader().use { reader ->
            val tree = tomlMapper.readTree(reader) as ObjectNode
            tree.fields().asSequence().map { (appName, value) ->
                appName to parseValues(appName, value as ObjectNode)
            }.toMap()
        }
    }

    private fun parseValues(appName: String, appConfig: ObjectNode): List<PropertyItem> {
        return appConfig.fields().asSequence().map { (key, value) ->
            PropertyItem(appName, key, 1L, value.asText())
        }.toList()
    }

    private fun parseProperties(): Map<String, List<PropertyItem>> {
        return openReader().use { reader ->
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

    private fun openReader(): Reader {
        val path = configurationSource.path
        return when (configurationSource.sourceType) {
            FileSourceType.CLASSPATH -> {
                this::class.java.getResourceAsStream(path)?.reader()
                    ?: error("Cannot find file $path in classpath")
            }

            FileSourceType.FILE_SYSTEM -> {
                val filePath = Paths.get(path)
                require(Files.exists(filePath)) { "Cannot find file $filePath in file system" }
                Files.newBufferedReader(filePath)
            }

            FileSourceType.GOOGLE_STORAGE -> {
                val channel = StorageOptions.getDefaultInstance().service.reader(BlobId.fromGsUtilUri(path))
                Channels.newReader(channel, StandardCharsets.UTF_8)
            }
        }
    }

    override fun subscribeToProperties(appName: String): ConfigurationSnapshot {
        return ConfigurationSnapshot(properties[appName] ?: emptyList())
    }

    override fun stop() = Unit
}
