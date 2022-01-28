package com.configset.client

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import kotlin.test.assertFails

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
        assertFails {
            processor.parse("prefix\${my-app1\\some-value1")
        }
    }

    @Test
    fun testFailEmptyPar() {
        assertFails {
            processor.parse("\${}")
        }
    }

    @Test
    fun testNoAppName() {
        assertFails {
            processor.parse("\${\\name}")
        }
    }

    @Test
    fun testNoAppValue() {
        assertFails {
            processor.parse("\${appName\\}")
        }
    }
}