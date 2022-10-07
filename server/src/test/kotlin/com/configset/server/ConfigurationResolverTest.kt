package com.configset.server

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

private const val defaultApp = "music"
private const val targetApp = "web"

class ConfigurationResolverTest {

    private val properties = mapOf(
            "srvd1" to 1,
            "host-music" to 2,
            "host-web" to 3,
            "test.group" to 4,
            "group" to 5,
            "anton.ermak" to 6
    )

    @Test
    fun testExact() {
        resolveProperty(properties, "srvd1", defaultApp, targetApp) shouldBeEqualTo 1
    }

    @Test
    fun testByDefaultAppName() {
        resolveProperty(properties, "srvd2", defaultApp, targetApp) shouldBeEqualTo 2
    }

    @Test
    fun testByTargetAppName() {
        resolveProperty(properties, "srvd2", "anotherDefault", targetApp) shouldBeEqualTo 3
    }

    @Test
    fun testByAnotherTargetAppName() {
        resolveProperty(properties, "srvd2", "anotherDefault", "anotherTarget") shouldBeEqualTo null
    }

    @Test
    fun testSplit_TestGroup() {
        resolveProperty(properties, "1.test.group", "anotherDefault", "anotherTarget") shouldBeEqualTo 4
    }

    @Test
    fun testSplit_Group() {
        resolveProperty(properties, "group", "anotherDefault", "anotherTarget") shouldBeEqualTo 5
    }

    @Test
    fun testFull() {
        resolveProperty(properties, "anton.ermak", "anotherDefault", "anotherTarget") shouldBeEqualTo 6
    }
}