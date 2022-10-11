package com.configset.dashboard

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.http.Context
import io.javalin.http.Handler

class RequestExtender(private val objectMapper: ObjectMapper) : Handler {

    companion object {
        private const val OBJECT_MAPPER_KEY = "OBJECT_MAPPER"

        val Context.objectMapper: ObjectMapper
            get() = (this.attribute(OBJECT_MAPPER_KEY) as? ObjectMapper)!!
    }

    override fun handle(ctx: Context) {
        ctx.attribute(OBJECT_MAPPER_KEY, objectMapper)
    }
}