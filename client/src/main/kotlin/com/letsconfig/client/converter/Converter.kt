package com.letsconfig.client.converter

interface Converter<out T> {
    fun convert(value: String): T
}