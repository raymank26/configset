package com.configset.client

import com.configset.client.converter.Converter
import com.configset.client.reflect.PropertyInfo
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConfPropertyInterfaceTest : BaseClientTest() {

    private lateinit var configInterface: SampleInterface

    @BeforeEach
    fun beforeTest() {
        configInterface = defaultConfiguration.getConfPropertyInterface(
            "test.interface",
            SampleInterface::class.java
        )
    }

    @Test
    fun shouldCreateSimpleInterface() {
        // then
        configInterface.someValue() shouldBeEqualTo ""

        clientUtil.pushPropertyUpdate(
            APP_NAME, "test.interface", """
            someValue=Hello
        """.trimIndent()
        )

        Awaitility.await().untilAsserted {
            configInterface.someValue() shouldBeEqualTo "Hello"
        }
    }

    @Test
    fun shouldManageConfPropertyReturnType() {
        // then
        configInterface.nestedValue().getValue() shouldBeEqualTo null

        clientUtil.pushPropertyUpdate(
            APP_NAME, "test.interface", """
            nestedValue=6
        """.trimIndent()
        )

        var receivedNewValue: Int? = null
        configInterface.nestedValue().subscribe { newValue ->
            receivedNewValue = newValue
        }

        Awaitility.await().untilAsserted {
            receivedNewValue shouldBeEqualTo 6
        }

        Awaitility.await().untilAsserted {
            configInterface.nestedValue().getValue() shouldBeEqualTo 6
        }
    }

    @Test
    fun shouldHandleCustomMethodName() {
        clientUtil.pushPropertyUpdate(
            APP_NAME, "test.interface", """
            customMethodName=Here
        """.trimIndent()
        )

        Awaitility.await().untilAsserted {
            configInterface.foo() shouldBeEqualTo "Here"
        }
    }

    @Test
    fun shouldHandleCustomConverter() {
        clientUtil.pushPropertyUpdate(
            APP_NAME, "test.interface", """
            custom=Sample data
        """.trimIndent()
        )

        Awaitility.await().untilAsserted {
            configInterface.custom() shouldBeEqualTo Foo("Sample data")
        }
    }

    @Test
    fun shouldResolveLinks() {
        clientUtil.pushPropertyUpdate(APP_NAME, "linked.property", "linked value")
        val link = buildString {
            append("$")
            append("{")
            append(APP_NAME)
            append("\\")
            append("linked.property")
            append("}")
        }
        val propertyValue = """
            someValue=$link
        """.trimIndent()
        clientUtil.pushPropertyUpdate(APP_NAME, "test.interface", propertyValue)

        Awaitility.await().untilAsserted {
            configInterface.someValue() shouldBeEqualTo "linked value"
        }
    }

    @Test
    fun noReturnValueShouldBeImmediatelyReported() {
        assertThrows<Exception> {
            defaultConfiguration.getConfPropertyInterface(
                "test.interface",
                NoReturnValueInterface::class.java
            )
        }
    }

    @Test
    fun noPropertyInfoOnMethodShouldBeImmediatelyReported() {
        assertThrows<Exception> {
            defaultConfiguration.getConfPropertyInterface(
                "test.interface",
                MethodWithoutAnnotationInterface::class.java
            )
        }
    }
}

interface SampleInterface {

    @PropertyInfo
    fun someValue(): String?

    @PropertyInfo
    fun nestedValue(): ConfProperty<Int?>

    @PropertyInfo(name = "customMethodName")
    fun foo(): String

    @PropertyInfo(converter = CustomConverter::class)
    fun custom(): Foo
}

interface NoReturnValueInterface {

    @PropertyInfo
    fun someValue()
}

interface MethodWithoutAnnotationInterface {

    fun someValue()
}

private class CustomConverter : Converter<Foo> {

    override fun convert(value: String): Foo {
        return Foo(value)
    }
}

data class Foo(val data: String)

