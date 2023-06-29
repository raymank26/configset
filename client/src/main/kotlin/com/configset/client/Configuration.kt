package com.configset.client

import com.configset.client.converter.Converter

interface Configuration {
    fun getConfiguration(appName: String): Configuration
    fun <T> getConfProperty(name: String, converter: Converter<T>): ConfProperty<T?>
    fun <T> getConfPropertyNotNull(name: String, converter: Converter<T>): ConfProperty<T>
    fun <T : Any> getConfProperty(name: String, converter: Converter<T>, defaultValue: T): ConfProperty<T>
    fun <T : Any> getConfPropertyInterface(name: String, cls: Class<T>): T
}
