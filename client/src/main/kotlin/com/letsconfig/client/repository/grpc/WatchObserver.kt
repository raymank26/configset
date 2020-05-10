package com.letsconfig.client.repository.grpc

import com.letsconfig.client.PropertyItem
import com.letsconfig.sdk.extension.createLoggerStatic
import com.letsconfig.sdk.proto.PropertiesChangesResponse
import io.grpc.stub.StreamObserver
import java.util.*

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
            if (propertyItemProto.updateType == com.letsconfig.sdk.proto.PropertyItem.UpdateType.DELETE) {
                updates.add(PropertyItem.Deleted(propertyItemProto.applicationName,
                        propertyItemProto.propertyName, propertyItemProto.version))
            } else {
                updates.add(PropertyItem.Updated(propertyItemProto.applicationName,
                        propertyItemProto.propertyName, propertyItemProto.version, propertyItemProto.propertyValue))
            }
        }
        onUpdate.invoke(value.applicationName, updates, lastVersion)
    }

    override fun onError(t: Throwable?) {
        if (!isStopped) {
            onEnd.invoke()
        }
    }

    override fun onCompleted() {
        onEnd.invoke()
    }
}