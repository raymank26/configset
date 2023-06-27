package com.configset.client

import com.configset.client.converter.Converters
import com.configset.client.repository.ConfigurationRepository
import org.amshove.kluent.invoking
import org.amshove.kluent.should
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.junit.Assert
import org.junit.jupiter.api.Test

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
            configurationSnapshot = createOnePropertySnapshot("app", "property.name", expected)
        )
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
            configurationSnapshot = createOnePropertySnapshot(
                "app", "property.name",
                "some value \${linkApp\\linkName} suffix"
            )
        )

        property.getValue() shouldBeEqualTo "some value linkValue suffix"
    }

    private fun createOnePropertySnapshot(
        appName: String,
        propertyName: String,
        propertyValue: String
    ): ConfigurationSnapshot {
        return ConfigurationSnapshot(listOf(PropertyItem(appName, propertyName, 1L, propertyValue)))
    }

    @Test
    fun testUnsubscription() {
        val linkedPropertySnapshot = createOnePropertySnapshot("linkApp", "linkName", "linkValue")
        val linkedProperty: ObservableConfProperty<String?> = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = { _, _ -> error("should not be called") },
            name = "linkName",
            defaultValue = "linkValue",
            converter = Converters.STRING,
            configurationSnapshot = linkedPropertySnapshot
        )

        val targetPropertyObservable = createOnePropertySnapshot(
            "linkApp", "property.name",
            "some value \${linkApp\\linkName} suffix"
        )
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
            configurationSnapshot = targetPropertyObservable
        )

        targetPropertyObservable.update(listOf(PropertyItem("linkApp", "property.name", 2L, "new value")))

        var subscriptionCalled = false
        targetProperty.subscribe {
            subscriptionCalled = true
        }
        linkedPropertySnapshot.update(listOf(PropertyItem("linkApp", "linked.name", 2L, "new linked value")))

        subscriptionCalled shouldBe false
    }

    @Test
    fun testLinkUpdateRecalculation() {
        val linkedPropertyObservable = createOnePropertySnapshot("linkApp", "linked.name", "linkValue")
        val linkedProperty: ObservableConfProperty<String?> = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = { _, _ -> error("should not be called") },
            name = "linked.name",
            defaultValue = "linkValue",
            converter = Converters.STRING,
            configurationSnapshot = linkedPropertyObservable
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
            configurationSnapshot = createOnePropertySnapshot(
                "linkApp", "property.name",
                "some value \${linkApp\\linkName} suffix"
            )
        )
        property.getValue() shouldBeEqualTo "some value linkValue suffix"

        var subscriberInvocations = 0
        property.subscribe {
            subscriberInvocations++
        }

        linkedPropertyObservable.update(listOf(PropertyItem("linkApp", "linked.name", 2L, "link updated")))

        property.getValue() shouldBeEqualTo "some value link updated suffix"

        subscriberInvocations shouldBeEqualTo 1
    }

    @Test
    fun testFailedConverter() {
        val property = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = { _, _ ->
                Assert.fail("should not be called") as Nothing
            },
            name = "property.name",
            defaultValue = null,
            converter = Converters.INTEGER,
            configurationSnapshot = createOnePropertySnapshot("linkApp", "property.name", "Non integer")
        )

        property.getValue().shouldBeNull()
    }

    @Test
    fun testRecursiveResolutionFails() {
        val content = """
            prop1=${'$'}{foo\prop2}
            prop2=${'$'}{foo\prop1}
        """.trimIndent()
        val config: ConfigurationRegistry<Configuration> =
            ConfigurationRegistryFactory.getConfiguration(TextConfigurationRepository(content))
        invoking {
            config.getConfiguration("foo").getConfProperty("prop2", Converters.STRING).getValue()
        } shouldThrow IllegalStateException::class withMessage "Recursive resolution found"
    }
}

private class TextConfigurationRepository(val text: String) : ConfigurationRepository {

    override fun start() {
    }

    override fun subscribeToProperties(appName: String): ConfigurationSnapshot {
        val properties = text.lineSequence()
            .map { it.split('=') }
            .map { PropertyItem(appName, it[0], 1, it[1]) }
            .toList()

        return ConfigurationSnapshot(properties)
    }

    override fun stop() {
    }
}
