package com.configset.dashboard

data class ServerApiGatewayException(val type: ServerApiGatewayErrorType) : Exception()

enum class ServerApiGatewayErrorType {
    CONFLICT,
    APPLICATION_NOT_FOUND,
    PROPERTY_NOT_FOUND,
    HOST_NOT_FOUND
}

fun ServerApiGatewayErrorType.throwException(): Nothing = throw ServerApiGatewayException(this)
