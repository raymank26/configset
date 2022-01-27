package com.configset.client

import com.configset.client.converter.Converters
import org.amshove.kluent.should
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.Test
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
    fun testUnsubscription() {
        val linkedPropertyObservable = DynamicValue<String?, String?>("linkValue", ChangingObservable())
        val linkedProperty: ObservableConfProperty<String?> = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = { _, _ -> error("should not be called") },
            name = "linked.name",
            defaultValue = "linkValue",
            converter = Converters.STRING,
            dynamicValue = linkedPropertyObservable
        )
        val targetPropertyObservable = DynamicValue<String?, String?>("some value \${linkApp\\linkName} suffix",
            ChangingObservable())
        val targetProperty = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = { appName, propName ->
                should { appName == "linkApp" }
                should { propName == "linkName" }
                linkedProperty
            },
            name = "property.name",
            defaultValue = null,
            converter = Converters.STRING,
            dynamicValue = targetPropertyObservable)

        targetPropertyObservable.observable.push("new value")

        var subscriptionCalled = false
        targetProperty.subscribe {
            subscriptionCalled = true
        }
        linkedPropertyObservable.observable.push("new linked value")

        subscriptionCalled shouldBe false
    }

    @Test
    fun testLinkUpdateRecalculation() {
        val linkedPropertyObservable = DynamicValue<String?, String?>("linkValue", ChangingObservable())
        val linkedProperty: ObservableConfProperty<String?> = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = { _, _ -> error("should not be called") },
            name = "linked.name",
            defaultValue = "linkValue",
            converter = Converters.STRING,
            dynamicValue = linkedPropertyObservable
        )
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

        var subscriberInvocations = 0
        property.subscribe {
            subscriberInvocations++
        }
//
        linkedPropertyObservable.observable.push("link updated")

        property.getValue() shouldBeEqualTo "some value link updated suffix"

        subscriberInvocations shouldBeEqualTo 1
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