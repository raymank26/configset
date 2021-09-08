package com.configset.client

import com.configset.client.converter.Converters
import org.amshove.kluent.should
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.Test
import java.util.*
import kotlin.test.fail

class ObservableConfPropertyTest {

    @Test
    fun testBasicEvaluation() {
        val expected = "234"
        val property = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = { _, _ -> error("should not be called") },
            name = "property.name",
            defaultValue = null,
            converter = Converters.INTEGER,
            dynamicValue = DynamicValue(expected, ChangingObservable()))
        property.getValue() shouldBeEqualTo expected.toInt()
    }

    @Test
    fun testLinkEvaluation() {
        val linkedProperty: ConstantConfProperty<String?> = ConstantConfProperty("linkValue")
        val property = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = { appName, propName ->
                should { appName == "linkApp" }
                should { propName == "linkName" }
                linkedProperty
            },
            name = "property.name",
            defaultValue = null,
            converter = Converters.STRING,
            dynamicValue = DynamicValue("some value \${linkApp\\linkName} suffix", ChangingObservable()))

        property.getValue() shouldBeEqualTo "some value linkValue suffix"
    }

    @Test
    fun testLinkUpdateRecalculation() {
        val linkedProperty: UpdatableConfProperty<String?> = UpdatableConfProperty("linkValue")
        val property = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = { appName, propName ->
                should { appName == "linkApp" }
                should { propName == "linkName" }
                linkedProperty
            },
            name = "property.name",
            defaultValue = null,
            converter = Converters.STRING,
            dynamicValue = DynamicValue("some value \${linkApp\\linkName} suffix", ChangingObservable()))
        property.getValue() shouldBeEqualTo "some value linkValue suffix"

        linkedProperty.setValue("link updated")

        property.getValue() shouldBeEqualTo "some value link updated suffix"

    }

    @Test
    fun testFailedConverter() {
        val property = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = { _, _ ->
                fail("should not be called")
            },
            name = "property.name",
            defaultValue = null,
            converter = Converters.INTEGER,
            dynamicValue = DynamicValue("Non integer", ChangingObservable()))

        property.getValue().shouldBeNull()
    }
}

private class UpdatableConfProperty<T>(value: T) : ConfProperty<T> {

    private var currentValue = value
    private var listeners: MutableSet<Subscriber<T>> = Collections.newSetFromMap(IdentityHashMap())

    override fun getValue(): T {
        return currentValue
    }

    fun setValue(value: T) {
        currentValue = value
        for (listener in listeners) {
            listener.process(value)
        }
    }

    override fun subscribe(listener: Subscriber<T>): Subscription {
        listeners.add(listener)
        return object : Subscription {
            override fun unsubscribe() {
                listeners.remove(listener)
            }
        }
    }
}