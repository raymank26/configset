package com.configset.client

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ConfigurationSourceUriParserTest {

    @Test
    fun shouldParseTomlSourceOnFileSystem() {
        val res = ConfigurationSourceUriParser.parse("file:///absolute/path?format=toml")

        res shouldBeInstanceOf ConfigurationSource.File::class.java
        val fileSource = (res as ConfigurationSource.File)

        fileSource.format shouldBeEqualTo FileFormat.TOML
        fileSource.path shouldBeEqualTo "/absolute/path"
        fileSource.location shouldBeEqualTo FileLocation.FILE_SYSTEM
    }

    @Test
    fun shouldParsePropertiesGoogleStorage() {
        val res = ConfigurationSourceUriParser.parse("gs://absolute/path?format=properties")

        res shouldBeInstanceOf ConfigurationSource.File::class.java
        val fileSource = (res as ConfigurationSource.File)

        fileSource.format shouldBeEqualTo FileFormat.PROPERTIES
        fileSource.path shouldBeEqualTo "gs://absolute/path"
        fileSource.location shouldBeEqualTo FileLocation.GOOGLE_STORAGE
    }

    @Test
    fun shouldParseGrpc() {
        val res = ConfigurationSourceUriParser.parse(
            "grpc://some-host:8082?deadlineMs=10000&hostName=someAppHost&defaultApplicationName=app"
        )
        res shouldBeInstanceOf ConfigurationSource.Grpc::class.java

        val grpcSource = res as ConfigurationSource.Grpc

        grpcSource.hostName shouldBeEqualTo "someAppHost"
        grpcSource.defaultApplicationName shouldBeEqualTo "app"
        grpcSource.deadlineMs shouldBeEqualTo 10000L
        grpcSource.backendHost shouldBeEqualTo "some-host"
        grpcSource.backendPort shouldBeEqualTo 8082
    }
}