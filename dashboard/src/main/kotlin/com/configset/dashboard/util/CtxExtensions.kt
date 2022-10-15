package com.configset.dashboard.util

import com.configset.dashboard.RequestExtender.Companion.objectMapper
import com.configset.sdk.auth.UserInfo
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.http.Context

fun Context.requestId() = this.formParam("requestId") ?: throw BadRequest("requestId")

fun Context.userInfoOrNull(): UserInfo? = this.attribute("user_info") as? UserInfo

fun Context.userInfo(): UserInfo = userInfoOrNull() ?: error("No userInfo found")

fun Context.formParamSafe(name: String) = this.formParam(name) ?: throw BadRequest("param.not.found", name)

fun Context.queryParamSafe(name: String) = this.queryParam(name) ?: throw BadRequest("queryParam.not.found", name)

fun Context.htmxRedirect(location: String) {
    this.header("HX-Redirect", location)
}

fun Context.htmlTriggerEvent(event: HtmxEvent) {
    val obj = this.objectMapper.createObjectNode()
    obj.set(event.name, objectMapper.valueToTree(event.payload) as ObjectNode) as ObjectNode
    this.header("HX-Trigger", objectMapper.writeValueAsString(obj))
}

fun Context.htmxShowAlert(text: String) {
    htmlTriggerEvent(HtmxEvent("showAlert", mapOf("text" to text)))
}

data class HtmxEvent(val name: String, val payload: Map<String, String>)
