package com.configset.client.converter

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class ConvertersTest {

    @Test
    fun testPropertiesConverter() {
        val result = Converters.PROPERTIES.convert("""
            str=value
            foo=bar
        """.trimIndent())

        result["str"] shouldBeEqualTo "value"
        result["foo"] shouldBeEqualTo "bar"
    }

    @Test
    fun testList() {
        Converters.LIST_LONG.convert("123,234") shouldBeEqualTo listOf(123L, 234L)
    }

    @Test
    fun testMap() {
        MapConverter(Converters.STRING, Converters.LONG).convert("a=123;b=102") shouldBeEqualTo mapOf("a" to 123L, "b" to 102L)
    }

    @Test
    fun testPojo() {
        val converter = PojoConverter { PojoExample(it.getString("foo"), it.getLong("bar"), it.getValueBy("bazz", Converters.LIST_STRING)) }
        converter.convert("""
            foo=someValue
            bar=123
            bazz=a,b,c,d
        """.trimIndent()) shouldBeEqualTo PojoExample("someValue", 123L, listOf("a", "b", "c", "d"))
    }

    @Test
    fun testCollectionConverter() {
        Converters.LIST_LONG.convert("") shouldBeEqualTo emptyList()
    }

    @Test
    fun testEnumConverter() {
        EnumConverter(EnumExample::class.java).convert("FOO") shouldBeEqualTo EnumExample.FOO
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEnumFail() {
        EnumConverter(EnumExample::class.java).convert("RANDOM")
    }

    @Test
    fun testMapMethod() {
        val converter = Converters.STRING.map { it.toLong() }
        converter.convert("123") shouldBeEqualTo 123L
    }

    @Test
    fun testChar() {
        Converters.CHAR.convert("1") shouldBeEqualTo '1'
    }

    @Test(expected = RuntimeException::class)
    fun testIllegalChar() {
        Converters.CHAR.convert("some string")
    }
}

private data class PojoExample(val foo: String, val bar: Long, val bazz: List<String>)

private enum class EnumExample {
    FOO,
    BAR
}