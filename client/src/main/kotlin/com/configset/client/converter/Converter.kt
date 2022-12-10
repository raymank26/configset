package com.configset.client.converter

interface Converter<out T> {
    fun convert(value: String): T
}
