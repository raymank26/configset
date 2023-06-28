package com.configset.client

import java.net.URI

object ConfigurationSourceUriParser {

    fun parse(uri: String): ConfigurationSource {
        val innerUri = URI(uri)
        val params = parseQueryParams(innerUri)
        val (location, path) = when {

            uri.startsWith("file://") -> {
                FileLocation.FILE_SYSTEM to innerUri.path
            }

            uri.startsWith("gs://") -> {
                FileLocation.GOOGLE_STORAGE to "gs://" + innerUri.host + innerUri.path
            }

            uri.startsWith("classpath://") -> {
                FileLocation.CLASSPATH to innerUri.path
            }

            uri.startsWith("grpc://") -> {
                val deadlineMs = getParam(params, "deadlineMs").toLong()
                val hostName = getParam(params, "hostName")
                val defaultApplicationName = getParam(params, "defaultApplicationName")
                return ConfigurationSource.Grpc(
                    hostName = hostName,
                    defaultApplicationName = defaultApplicationName,
                    backendHost = innerUri.host,
                    backendPort = innerUri.port,
                    deadlineMs = deadlineMs
                )
            }

            else -> error("Unknown URI format, uri = $uri")
        }

        val format = when (val rawFormat = getParam(params, "format")) {
            "toml" -> FileFormat.TOML
            "properties" -> FileFormat.PROPERTIES
            else -> error("Unknown format $rawFormat")
        }

        return ConfigurationSource.File(path, location, format)
    }

    private fun parseQueryParams(innerUri: URI): Map<String, String> {
        return innerUri.rawQuery.split("&")
            .map { it.split("=", limit = 2) }
            .associate { it[0] to it[1] }
    }

    private fun getParam(params: Map<String, String>, name: String): String {
        return params[name] ?: error("No '$name' key found in URI query")
    }
}