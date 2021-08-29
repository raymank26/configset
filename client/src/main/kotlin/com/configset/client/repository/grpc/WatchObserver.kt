package com.configset.client.repository.grpc

import com.configset.client.PropertyItem
import com.configset.sdk.extension.createLoggerStatic
import com.configset.sdk.proto.PropertiesChangesResponse
import com.configset.sdk.proto.PropertyItem.UpdateType
import io.grpc.stub.StreamObserver

private val LOG = createLoggerStatic<WatchObserver>()

class WatchObserver(
        private val onUpdate: (String, List<PropertyItem>, Long) -> Unit,
        private val onEnd: () -> Unit
) : StreamObserver<PropertiesChangesResponse> {

    @Volatile
    var isStopped = false

    override fun onNext(value: PropertiesChangesResponse) {
        val updates: MutableList<PropertyItem> = ArrayList()
        val lastVersion = value.lastVersion
        for (propertyItemProto in value.itemsList) {
            val propValue =
                if (propertyItemProto.updateType == UpdateType.DELETE) null else propertyItemProto.propertyValue
            updates.add(PropertyItem(propertyItemProto.applicationName, propertyItemProto.propertyName,
                propertyItemProto.version, propValue))
        }
        onUpdate.invoke(value.applicationName, updates, lastVersion)
    }

    override fun onError(t: Throwable?) {
        if (!isStopped) {
            onEnd.invoke()
        } else {
            LOG.warn("Exception in connection", t)
        }
    }

    override fun onCompleted() {
        onEnd.invoke()
    }
}