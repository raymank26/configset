package com.configset.client.converter

import com.configset.client.reflect.PropertyInfo
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.junit.jupiter.api.Test

class ClsConverterTest {

    @Test
    fun shouldCreateInterfaceSnapshot() {
        val cls = ClsConverter(CustomInterface::class.java).convert(
            """
            firstConfig=12
            secondConfig=foobar
            listLong=23,34
            """.trimIndent()
        )

        cls.firstConfig() shouldBeEqualTo 12
        cls.secondConfig() shouldBeEqualTo "foobar"
        cls.thirdDefault() shouldBeEqualTo 2389
        cls.listLong() shouldBeEqualTo listOf(23L, 34L)
        cls.nullableProperty() shouldBeEqualTo null
        invoking {
            cls.notNullProperty()
        } shouldThrow Exception::class withMessage "A method \"notNullProperty\" returns null but it's declared as non-nullable type"
    }
}

interface CustomInterface {

    @PropertyInfo
    fun firstConfig(): Int

    @PropertyInfo
    fun secondConfig(): String

    @PropertyInfo(defaultValue = ["2389"])
    fun thirdDefault(): Int

    @PropertyInfo(converter = ListLongConverter::class)
    fun listLong(): List<Long>

    @PropertyInfo
    fun nullableProperty(): String?

    @PropertyInfo
    fun notNullProperty(): String
}

class ListLongConverter : Converter<List<Long>> {

    override fun convert(value: String): List<Long> {
        return Converters.LIST_LONG.convert(value)
    }
}
