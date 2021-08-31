package com.configset.client.repository.grpc

import com.configset.client.ChangingObservable
import com.configset.client.PropertyItem
import com.configset.client.Subscriber
import com.configset.client.metrics.LibraryMetrics
import com.configset.client.repository.ConfigApplication
import com.configset.client.repository.ConfigurationRepository
import com.configset.sdk.extension.createLoggerStatic
import com.configset.sdk.proto.SubscribeApplicationRequest
import com.configset.sdk.proto.WatchRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private const val INITIAL_TIMEOUT_SEC: Long = 10L
private val LOG = createLoggerStatic<GrpcConfigurationRepository>()

class GrpcConfigurationRepository(
        private val applicationHostname: String,
        private val defaultApplicationName: String,
        private val serverHostname: String,
        private val serverPort: Int,
        private val libraryMetrics: LibraryMetrics
) : ConfigurationRepository {

    private lateinit var grpcRemoteConnector: GrpcRemoteConnector

    @Synchronized
    override fun start() {
        grpcRemoteConnector = GrpcRemoteConnector(libraryMetrics, applicationHostname, serverHostname, serverPort)
        grpcRemoteConnector.init()
    }

    override fun subscribeToProperties(appName: String): ConfigApplication {
        val currentObservable = ChangingObservable<List<PropertyItem>>()
        synchronized(this) {
            grpcRemoteConnector.subscribeForChanges(appName, currentObservable)
            val subscribeRequest = SubscribeApplicationRequest
                .newBuilder()
                .setApplicationName(appName)
                .setDefaultApplicationName(defaultApplicationName)
                .setHostName(applicationHostname)
                .build()
            grpcRemoteConnector.sendRequest(WatchRequest.newBuilder()
                .setType(WatchRequest.Type.SUBSCRIBE_APPLICATION)
                .setSubscribeApplicationRequest(subscribeRequest)
                .build())
        }

        val future = CompletableFuture<List<PropertyItem>>()
        currentObservable.onSubscribe(object : Subscriber<List<PropertyItem>> {
            override fun process(value: List<PropertyItem>) {
                future.complete(value)
            }
        })
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

    @Synchronized
    override fun stop() {
        grpcRemoteConnector.stop()
    }
}

data class WatchState(
    val appName: String,
    var lastVersion: Long,
    val observable: ChangingObservable<List<PropertyItem>>,
)