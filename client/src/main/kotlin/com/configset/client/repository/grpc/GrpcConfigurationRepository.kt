package com.configset.client.repository.grpc

import com.configset.client.ConfigurationSnapshot
import com.configset.client.repository.ConfigurationRepository
import com.configset.common.client.extension.createLoggerStatic
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private const val INITIAL_TIMEOUT_SEC: Long = 10L
private val LOG = createLoggerStatic<GrpcConfigurationRepository>()

class GrpcConfigurationRepository(
    private val applicationHostname: String,
    private val defaultApplicationName: String,
    private val grpcClientFactory: GrpcClientFactory,
    private val reconnectionTimeoutMs: Long,
) : ConfigurationRepository {

    private lateinit var grpcRemoteConnector: GrpcRemoteConnector

    override fun start() {
        grpcRemoteConnector = GrpcRemoteConnector(applicationHostname, reconnectionTimeoutMs, grpcClientFactory)
        grpcRemoteConnector.resubscribe()
    }

    override fun subscribeToProperties(appName: String): ConfigurationSnapshot {
        val future = CompletableFuture<Unit>()
        val snapshot = ConfigurationSnapshot(emptyList())
        var initialPropertiesSize: Int = -1
        grpcRemoteConnector.subscribeForChanges(appName, defaultApplicationName) { properties ->
            if (initialPropertiesSize < 0) {
                initialPropertiesSize = properties.size
            }
            snapshot.update(properties)
            future.complete(Unit)
        }
        while (true) {
            try {
                future.get(INITIAL_TIMEOUT_SEC, TimeUnit.SECONDS)
                break
            } catch (e: TimeoutException) {
                LOG.warn("Unable to get initial configuration for seconds = $INITIAL_TIMEOUT_SEC seconds, retrying..")
            }
        }
        LOG.info("Initial properties for app = $appName received with size = $initialPropertiesSize")
        return snapshot
    }

    override fun stop() {
        grpcRemoteConnector.stop()
    }
}

data class WatchState(
    val appName: String,
    val lastVersion: Long,
    val updateCallback: PropertiesUpdatedCallback,
)
