package com.configset.client.repository.grpc

import com.configset.client.PropertyItem
import com.configset.client.proto.PropertiesChangesResponse
import com.configset.client.proto.SubscribeApplicationRequest
import com.configset.client.proto.UpdateReceived
import com.configset.client.proto.WatchRequest
import com.configset.common.client.extension.createLoggerStatic
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Consumer
import kotlin.concurrent.read
import kotlin.concurrent.thread
import kotlin.concurrent.write

private val LOG = createLoggerStatic<GrpcRemoteConnector>()

class GrpcRemoteConnector(
    private val applicationHostname: String,
    private val reconnectionTimeoutMs: Long,
    grpcClientFactory: GrpcClientFactory,
) {

    private val appWatchMappers: MutableMap<String, WatchState> = HashMap()
    private var stopped = false
    private val requestExecutor = Executors.newCachedThreadPool(
        ThreadFactoryBuilder()
            .setNameFormat("request-executor-%d")
            .setDaemon(true)
            .build()
    )
    private val updateExecutor = Executors.newCachedThreadPool(
        ThreadFactoryBuilder()
            .setNameFormat("update-executor-%d")
            .setDaemon(true)
            .build()
    )
    private val sendRwLock = ReentrantReadWriteLock()
    private val asyncClient = grpcClientFactory.createAsyncClient()
    private lateinit var observer: StreamObserver<WatchRequest>

    @Synchronized
    fun resubscribe() {
        if (stopped) {
            return
        }
        observer = asyncClient.watchChanges(object : StreamObserver<PropertiesChangesResponse> {
            override fun onNext(value: PropertiesChangesResponse) {
                processUpdate(value)
            }

            override fun onError(t: Throwable) {
                LOG.warn("Exception in communication", t)
                emitReconnection()
            }

            override fun onCompleted() {
                LOG.debug("Completed called")
                emitReconnection()
            }
        })
        for (watchState in appWatchMappers) {
            val appName = watchState.value.appName
            val subscribeRequest = SubscribeApplicationRequest
                .newBuilder()
                .setApplicationName(appName)
                .setHostName(applicationHostname)
                .setLastKnownVersion(watchState.value.lastVersion)
                .build()
            sendWatchRequestAsync(
                WatchRequest.newBuilder()
                    .setType(WatchRequest.Type.SUBSCRIBE_APPLICATION)
                    .setSubscribeApplicationRequest(subscribeRequest)
                    .build()
            )
            LOG.info("Resubscribed to app = $appName and lastVersion = ${watchState.value.lastVersion}")
        }
        LOG.info("Watch subscription is (re)initialized")
    }

    @Synchronized
    fun subscribeForChanges(
        appName: String,
        defaultApplicationName: String,
        updateCallback: PropertiesUpdatedCallback
    ) {
        appWatchMappers[appName] = WatchState(appName, -1, updateCallback)
        val subscribeRequest = SubscribeApplicationRequest
            .newBuilder()
            .setApplicationName(appName)
            .setDefaultApplicationName(defaultApplicationName)
            .setHostName(applicationHostname)
            .build()
        sendWatchRequestAsync(
            WatchRequest.newBuilder()
                .setType(WatchRequest.Type.SUBSCRIBE_APPLICATION)
                .setSubscribeApplicationRequest(subscribeRequest)
                .build()
        )
    }

    private fun sendWatchRequestAsync(watchRequest: WatchRequest) {
        if (stopped) {
            return
        }
        requestExecutor.submit {
            while (true) {
                try {
                    sendRwLock.read {
                        if (!stopped) {
                            observer.onNext(watchRequest)
                        }
                    }
                    return@submit
                } catch (e: Exception) {
                    LOG.warn("Cannot send watch request", e)
                    Thread.sleep(2000)
                }
            }
        }
    }

    private fun emitReconnection() {
        thread(name = "Grpc-watch-reconnection", isDaemon = true) {
            Thread.sleep(reconnectionTimeoutMs)
            resubscribe()
        }
    }

    fun stop() {
        sendRwLock.write {
            stopped = true
        }
        observer.onCompleted()
        requestExecutor.shutdownNow()
        updateExecutor.shutdownNow()
        (asyncClient.channel as ManagedChannel).shutdown()
    }

    @Synchronized
    private fun processUpdate(value: PropertiesChangesResponse) {
        val updates: MutableList<PropertyItem> = ArrayList()
        val lastVersion = value.lastVersion
        for (propertyItemProto in value.itemsList) {
            val propValue = if (propertyItemProto.deleted) null else propertyItemProto.propertyValue
            updates.add(
                PropertyItem(
                    propertyItemProto.applicationName, propertyItemProto.propertyName,
                    propertyItemProto.version, propValue
                )
            )
        }

        val appName = value.applicationName
        val watchState: WatchState = appWatchMappers[appName]!!
        if (lastVersion <= watchState.lastVersion) {
            if (updates.isNotEmpty()) {
                LOG.debug(
                    """Obsolete value has come, known version = ${watchState.lastVersion},
                        | received = $lastVersion, applicationName = $appName, updateSize = ${updates.size}"""
                        .trimMargin()
                )
            }
            return
        }
        val filteredUpdates = updates.filter { it.version > watchState.lastVersion }
        if (filteredUpdates.size != updates.size) {
            LOG.debug(
                """Some updates where filtered (they are obsolete), before size = ${updates.size},
                | after size = ${filteredUpdates.size}"""
                    .trimMargin()
            )
        }
        LOG.info("Configuration updated {}", filteredUpdates)
        updateExecutor.execute {
            try {
                watchState.updateCallback.accept(filteredUpdates)
            } catch (e: Throwable) {
                LOG.warn("Unable to process updates for app = $appName", e)
            }
        }
        appWatchMappers[appName] = watchState.copy(lastVersion = lastVersion)
        sendWatchRequestAsync(
            WatchRequest.newBuilder()
                .setType(WatchRequest.Type.UPDATE_RECEIVED)
                .setUpdateReceived(
                    UpdateReceived.newBuilder()
                        .setApplicationName(appName)
                        .setVersion(lastVersion)
                        .build()
                )
                .build()
        )
    }
}

fun interface PropertiesUpdatedCallback : Consumer<List<PropertyItem>>
