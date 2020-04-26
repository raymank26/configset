package com.letsconfig.dashboard

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin

class CreateApplicationTest {

    private lateinit var server: JavalinServer

    @Before
    fun setUp() {
        val koinApp = startKoin {
            modules(mainModule)
        }.properties(mapOf(
                Pair("config_server.hostname", "localhost"),
                Pair("config_server.port", 8988),
                Pair("dashboard.port", 9299)
        ))
        Runtime.getRuntime().addShutdownHook(Thread {
            koinApp.close()
        })
        server = koinApp.koin.get<JavalinServer>()
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun createApplication() {

    }
}