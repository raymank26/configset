package com.letsconfig.dashboard

import io.javalin.Javalin


object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val app = Javalin.create().start(8188)
        app.get("/api/hello") { ctx -> ctx.result("Hello World") }
    }
}