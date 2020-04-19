package com.letsconfig.client.repository

import com.letsconfig.client.ChangingObservable
import com.letsconfig.client.DynamicValue
import com.letsconfig.client.PropertyItem
import com.letsconfig.sdk.proto.ConfigurationServiceGrpc
import com.letsconfig.sdk.proto.PropertiesChangesResponse
import com.letsconfig.sdk.proto.SubscribeApplicationRequest
import com.letsconfig.sdk.proto.SubscriberInfoRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import java.util.*

class GrpcConfigurationRepository(
        private val applicationHostname: String,
        serverHostname: String,
        serverPort: Int
) : ConfigurationRepository {

    private val asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub
    private val blockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub

    init {
        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(serverHostname, serverPort)
                .usePlaintext()
                .build()
        asyncClient = ConfigurationServiceGrpc.newStub(channel)
        blockingClient = ConfigurationServiceGrpc.newBlockingStub(channel)
    }

    override fun subscribeToProperties(appName: String): DynamicValue<List<PropertyItem.Updated>, List<PropertyItem>> {
        val subscriberId = UUID.randomUUID().toString()
        val response = blockingClient.subscribeApplication(SubscribeApplicationRequest
                .newBuilder()
                .setApplicationName(appName)
                .setHostName(applicationHostname)
                .setSubscriberId(subscriberId)
                .build())

        val snapshot: MutableList<PropertyItem.Updated> = ArrayList()

        for (propertyItemProto in response.itemsList) {
            snapshot.add(PropertyItem.Updated(propertyItemProto.applicationName, propertyItemProto.propertyName,
                    propertyItemProto.version, propertyItemProto.propertyValue))
        }

        val updateObservable = ChangingObservable<List<PropertyItem>>()

        asyncClient.watchChanges(SubscriberInfoRequest.newBuilder().setId(subscriberId).build(), object : StreamObserver<PropertiesChangesResponse> {
            override fun onNext(value: PropertiesChangesResponse) {
                val updates: MutableList<PropertyItem> = ArrayList()
                for (propertyItemProto in value.itemsList) {
                    if (propertyItemProto.propertyValue == null) {
                        updates.add(PropertyItem.Deleted(propertyItemProto.applicationName,
                                propertyItemProto.propertyName, propertyItemProto.version))
                    } else {
                        updates.add(PropertyItem.Updated(propertyItemProto.applicationName,
                                propertyItemProto.propertyName, propertyItemProto.version, propertyItemProto.propertyValue))
                    }
                }
                updateObservable.setValue(updates)
            }

            override fun onError(t: Throwable?) {
                TODO("Not yet implemented")
            }

            override fun onCompleted() {
                TODO("Not yet implemented")
            }
        })

        return DynamicValue(snapshot, updateObservable)
    }
}