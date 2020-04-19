package com.letsconfig.client

import com.letsconfig.client.converter.Converter

interface Configuration {
    fun getConfiguration(appName: String): Configuration
    fun <T> getConfProperty(name: String, converter: Converter<T>): ConfProperty<T?>
    fun <T> getConfProperty(name: String, converter: Converter<T>, defaultValue: T): ConfProperty<T>
}