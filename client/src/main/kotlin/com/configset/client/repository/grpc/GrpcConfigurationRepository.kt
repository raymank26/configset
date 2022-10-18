package com.configset.client.repository.grpc

import com.configset.client.ChangingObservable
import com.configset.client.PropertyItem
import com.configset.client.repository.ConfigApplication
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
        grpcRemoteConnector = GrpcRemoteConnector(applicationHostname, grpcClientFactory, reconnectionTimeoutMs)
        grpcRemoteConnector.init()
    }

    override fun subscribeToProperties(appName: String): ConfigApplication {
        val currentObservable = ChangingObservable<List<PropertyItem>>()
        grpcRemoteConnector.subscribeForChanges(appName, defaultApplicationName, currentObservable)

        val future = CompletableFuture<List<PropertyItem>>()
        currentObservable.onSubscribe { value -> future.complete(value) }
        var initialProperties: List<PropertyItem>
        while (true) {
            try {
                initialProperties = future.get(INITIAL_TIMEOUT_SEC, TimeUnit.SECONDS)
                break
            } catch (e: TimeoutException) {
                LOG.warn("Unable to get initial configuration for seconds = $INITIAL_TIMEOUT_SEC seconds, retrying..")
            }
        }
        LOG.info("Initial properties for app = $appName received with size = ${initialProperties.size}")
        return ConfigApplication(appName, initialProperties, currentObservable)
    }

    override fun stop() {
        grpcRemoteConnector.stop()
    }
}

data class WatchState(
    val appName: String,
    var lastVersion: Long,
    val observable: ChangingObservable<List<PropertyItem>>,
)