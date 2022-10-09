package com.configset.client

import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.junit.Test

class ConfigPropertyLinkProcessorTest {
    private val processor = ConfigPropertyLinkProcessor.INSTANCE

    @Test
    fun testParsingSimple() {
        processor.parse("str") shouldBeEqualTo TokenList(listOf(Text("str")))
    }

    @Test
    fun testParsingWithLinks() {
        val parseResult = processor.parse("prefix\${my-app1\\some-value1}suffix \${my-app2\\some-value2}")
        parseResult shouldBeEqualTo TokenList(listOf(
            Text("prefix"),
            Link("my-app1", "some-value1"),
            Text("suffix "),
            Link("my-app2", "some-value2"),
        ))
    }

    @Test
    fun testFailsEndPar() {
        invoking {
            processor.parse("prefix\${my-app1\\some-value1")
        } shouldThrow Exception::class
    }

    @Test
    fun testFailEmptyPar() {
        invoking {
            processor.parse("\${}")
        } shouldThrow Exception::class
    }

    @Test
    fun testNoAppName() {
        invoking {
            processor.parse("\${\\name}")
        } shouldThrow Exception::class
    }

    @Test
    fun testNoAppValue() {
        invoking {
            processor.parse("\${appName\\}")
        } shouldThrow Exception::class
    }
}