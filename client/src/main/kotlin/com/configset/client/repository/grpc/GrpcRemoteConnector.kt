package com.configset.client.repository.grpc

import com.configset.client.ChangingObservable
import com.configset.client.PropertyItem
import com.configset.client.proto.ConfigurationServiceGrpc
import com.configset.client.proto.PropertiesChangesResponse
import com.configset.client.proto.SubscribeApplicationRequest
import com.configset.client.proto.UpdateReceived
import com.configset.client.proto.WatchRequest
import com.configset.common.client.extension.createLoggerStatic
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.concurrent.write

private val LOG = createLoggerStatic<GrpcRemoteConnector>()

class GrpcRemoteConnector(
    private val applicationHostname: String,
    private val grpcClientFactory: GrpcClientFactory,
    private val reconnectionTimeoutMs: Long,
) {

    private val appWatchMappers: MutableMap<String, WatchState> = ConcurrentHashMap()
    private lateinit var persistentWatcher: PersistentWatcher

    fun init() {
        persistentWatcher = PersistentWatcher(
            grpcClientFactory.createAsyncClient(),
            reconnectionTimeoutMs = reconnectionTimeoutMs,
            changesCallback = { processUpdate(it) },
            resubscribeCallback = { resubscribe() }
        )
        resubscribe()
    }

    private fun resubscribe() {
        persistentWatcher.connect()
        for (watchState in appWatchMappers) {
            val appName = watchState.value.appName
            val subscribeRequest = SubscribeApplicationRequest
                .newBuilder()
                .setApplicationName(appName)
                .setHostName(applicationHostname)
                .setLastKnownVersion(watchState.value.lastVersion)
                .build()
            persistentWatcher.sendWatchRequest(
                WatchRequest.newBuilder()
                    .setType(WatchRequest.Type.SUBSCRIBE_APPLICATION)
                    .setSubscribeApplicationRequest(subscribeRequest)
                    .build()
            )
            LOG.info("Resubscribed to app = $appName and lastVersion = ${watchState.value.lastVersion}")
        }
        LOG.info("Watch subscription is (re)initialized")
    }

    fun subscribeForChanges(
        appName: String,
        defaultApplicationName: String,
        currentObservable: ChangingObservable<List<PropertyItem>>
    ) {
        appWatchMappers[appName] = WatchState(appName, -1, currentObservable)
        val subscribeRequest = SubscribeApplicationRequest
            .newBuilder()
            .setApplicationName(appName)
            .setDefaultApplicationName(defaultApplicationName)
            .setHostName(applicationHostname)
            .build()
        persistentWatcher.sendWatchRequest(
            WatchRequest.newBuilder()
                .setType(WatchRequest.Type.SUBSCRIBE_APPLICATION)
                .setSubscribeApplicationRequest(subscribeRequest)
                .build()
        )
    }

    fun stop() {
        persistentWatcher.stop()
    }

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
        watchState.observable.push(filteredUpdates)
        watchState.lastVersion = lastVersion
        persistentWatcher.sendWatchRequest(
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

class PersistentWatcher(
    private val asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub,
    private val reconnectionTimeoutMs: Long,
    private val changesCallback: (PropertiesChangesResponse) -> Unit,
    private val resubscribeCallback: () -> Unit,
) {

    @Volatile
    private var stopped = false
    private lateinit var observer: StreamObserver<WatchRequest>
    private val requestExecutors = Executors.newCachedThreadPool(ThreadFactoryBuilder().setDaemon(true).build())
    private val reconnectionLock = ReentrantLock()
    private val sendRwLock = ReentrantReadWriteLock()

    fun connect() {
        if (stopped) {
            return
        }
        observer = asyncClient.watchChanges(object : StreamObserver<PropertiesChangesResponse> {
            override fun onNext(value: PropertiesChangesResponse) {
                changesCallback.invoke(value)
            }

            override fun onError(t: Throwable) {
                if (!stopped) {
                    LOG.warn("Exception in communication", t)
                    emitReconnection()
                }
            }

            override fun onCompleted() {
                LOG.debug("Completed called")
                if (!stopped) {
                    emitReconnection()
                }
            }
        })
    }

    private fun emitReconnection() {
        thread(name = "Grpc-watch-reconnection") {
            Thread.sleep(reconnectionTimeoutMs)
            reconnectionLock.withLock(resubscribeCallback)
        }
    }

    fun sendWatchRequest(watchRequest: WatchRequest) {
        if (stopped) {
            return
        }
        requestExecutors.submit {
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

    fun stop() {
        sendRwLock.write {
            stopped = true
        }
        observer.onCompleted()
        requestExecutors.shutdownNow()
        (asyncClient.channel as ManagedChannel).shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
    }
}
