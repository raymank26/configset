package com.configset.client.repository.grpc

import com.configset.client.ChangingObservable
import com.configset.client.PropertyItem
import com.configset.client.metrics.LibraryMetrics
import com.configset.client.metrics.Metrics
import com.configset.sdk.extension.createLoggerStatic
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.sdk.proto.PropertiesChangesResponse
import com.configset.sdk.proto.PropertyItem.UpdateType
import com.configset.sdk.proto.SubscribeApplicationRequest
import com.configset.sdk.proto.UpdateReceived
import com.configset.sdk.proto.WatchRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private val LOG = createLoggerStatic<GrpcRemoteConnector>()

class GrpcRemoteConnector(
    private val libraryMetrics: LibraryMetrics,
    private val applicationHostname: String,
    private val serverHostname: String,
    private val serverPort: Int,
) : StreamObserver<PropertiesChangesResponse> {

    private val appWatchMappers: MutableMap<String, WatchState> = ConcurrentHashMap()

    private lateinit var asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub
    private lateinit var channel: ManagedChannel
    private lateinit var watchMethodApi: StreamObserver<WatchRequest>

    @Volatile
    private var isStopped = false

    @Synchronized
    fun init() {
        channel = ManagedChannelBuilder.forAddress(serverHostname, serverPort)
            .usePlaintext()
            .keepAliveTime(5000, TimeUnit.MILLISECONDS)
            .build()
        asyncClient = ConfigurationServiceGrpc.newStub(channel)

        watchMethodApi = asyncClient.watchChanges(this)

        for (watchState in appWatchMappers) {
            val appName = watchState.value.appName
            val subscribeRequest = SubscribeApplicationRequest
                .newBuilder()
                .setApplicationName(appName)
                .setHostName(applicationHostname)
                .setLastKnownVersion(watchState.value.lastVersion)
                .build()
            watchMethodApi.onNext(WatchRequest.newBuilder()
                .setType(WatchRequest.Type.SUBSCRIBE_APPLICATION)
                .setSubscribeApplicationRequest(subscribeRequest)
                .build())
            LOG.info("Resubscribed to app = $appName and lastVersion = ${watchState.value.lastVersion}")
        }
        LOG.info("Watch subscription is (re)initialized")
    }

    fun subscribeForChanges(appName: String, currentObservable: ChangingObservable<List<PropertyItem>>) {
        appWatchMappers[appName] = WatchState(appName, -1, currentObservable)
    }

    fun stop() {
        isStopped = true
        watchMethodApi.onCompleted()
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
    }

    fun sendRequest(watchRequest: WatchRequest) {
        watchMethodApi.onNext(watchRequest)
    }

    override fun onNext(value: PropertiesChangesResponse) {
        val updates: MutableList<PropertyItem> = ArrayList()
        val lastVersion = value.lastVersion
        for (propertyItemProto in value.itemsList) {
            val propValue =
                if (propertyItemProto.updateType == UpdateType.DELETE) null else propertyItemProto.propertyValue
            updates.add(PropertyItem(propertyItemProto.applicationName, propertyItemProto.propertyName,
                propertyItemProto.version, propValue))
        }

        val appName = value.applicationName
        val watchState: WatchState = appWatchMappers[appName]!!
        if (lastVersion <= watchState.lastVersion) {
            if (updates.isNotEmpty()) {
                LOG.debug("Obsolete value has come, known version = ${watchState.lastVersion}," +
                        "received = ${lastVersion}, applicationName = $appName, updateSize = ${updates.size}")
            }
            return
        }
        val filteredUpdates = updates.filter { it.version > watchState.lastVersion }
        if (filteredUpdates.size != updates.size) {
            LOG.debug("Some updates where filtered (they are obsolete)" +
                    ", before size = ${updates.size}, after size = ${filteredUpdates.size}")
            libraryMetrics.increment(Metrics.SKIPPED_OBSOLETE_UPDATES)
        }
        watchState.observable.setValue(filteredUpdates)
        watchState.lastVersion = lastVersion
        watchMethodApi.onNext(WatchRequest.newBuilder()
            .setType(WatchRequest.Type.UPDATE_RECEIVED)
            .setUpdateReceived(UpdateReceived.newBuilder()
                .setApplicationName(appName)
                .setVersion(lastVersion)
                .build())
            .build())
    }

    override fun onError(t: Throwable?) {
        LOG.warn("Exception on streaming data", t)
        reconnect()
    }

    override fun onCompleted() {
        reconnect()
    }

    private fun reconnect() {
        if (isStopped) {
            return
        }
        thread {
            LOG.info("Resubscribe started, waiting for 5 seconds")
            try {
                channel.shutdownNow()
            } catch (e: Exception) {
                LOG.warn("Exception during shutdown", e)
            }
            Thread.sleep(5000)
            init()
        }
    }
}