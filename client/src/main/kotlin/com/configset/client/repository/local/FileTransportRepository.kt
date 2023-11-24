package com.configset.client.repository.local

import com.configset.client.*
import com.configset.client.repository.ConfigurationRepository
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.StorageOptions
import org.apache.http.client.utils.URLEncodedUtils
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.io.Reader
import java.net.URI
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

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
            val propertyValue = if (value is ObjectNode) {
                serializeObjectToPropertyValue(value)
            } else {
                value.asText()
            }
            PropertyItem(appName, key, 1L, propertyValue)
        }.toList()
    }

    private fun serializeObjectToPropertyValue(value: ObjectNode): String {
        return value.fields().asSequence().map { (key, value) ->
            val textValue = value.asText()
            require(!textValue.contains("\n")) { "Nested multiline values are not supported" }
            "$key=$textValue"
        }.joinToString("\n")
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
        val uri = configurationSource.path
        return when (configurationSource.location) {
            FileLocation.CLASSPATH -> {
                this::class.java.getResourceAsStream(uri.path)?.reader()
                    ?: error("Cannot find file $uri in classpath")
            }

            FileLocation.FILE_SYSTEM -> {
                val filePath = Paths.get(uri.path)
                require(Files.exists(filePath)) { "Cannot find file $filePath in file system" }
                Files.newBufferedReader(filePath)
            }

            FileLocation.GOOGLE_STORAGE -> {
                val channel = StorageOptions.getDefaultInstance().service.reader(
                    BlobId.fromGsUtilUri("gs://" + uri.host + uri.path)
                )
                Channels.newReader(channel, StandardCharsets.UTF_8)
            }

            FileLocation.S3 -> {
                val params = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8)
                    .associate { it.name to it.value }
                val s3Client = S3Client.builder()
                    .apply {
                        if (params["forcePathStyle"] == "true") {
                            forcePathStyle(true)
                        }
                        region(Region.of(params["region"]))
                        params["endpointOverride"]?.let {
                            endpointOverride(URI(it))
                        }
                        credentialsProvider {
                            AwsBasicCredentials.create(params["accessKeyId"], params["secretKey"])
                        }
                    }
                    .build()
                s3Client.getObject(
                    GetObjectRequest.builder()
                        .bucket(uri.host)
                        .key(uri.path.drop(1))
                        .build()
                )
                    .reader()
            }
        }
    }

    override fun subscribeToProperties(appName: String): ConfigurationSnapshot {
        return ConfigurationSnapshot(properties[appName] ?: emptyList())
    }

    override fun stop() = Unit
}
