package com.letsconfig.dashboard

import io.javalin.Javalin

class JavalinServer {

    fun start() {
        val app = Javalin.create().start(8188)
        app.get("/api/hello") { ctx -> ctx.result("Hello World") }
    }
}