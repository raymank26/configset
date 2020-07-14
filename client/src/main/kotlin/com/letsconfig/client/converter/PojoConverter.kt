package com.letsconfig.client.converter

import java.io.StringReader
import java.util.*

class PojoConverter<T>(private val mapper: (ValuesProvider) -> T) : Converter<T> {
    override fun convert(value: String): T {
        return mapper.invoke(ProviderProperty(value))
    }
}

interface ValuesProvider {
    fun hasKey(key: String): Boolean
    fun getString(key: String): String
    fun getInt(key: String): Int = getString(key).toInt()
    fun getLong(key: String): Long = getString(key).toLong()
    fun <T> getValueBy(key: String, converter: Converter<T>): T = getString(key).let { converter.convert(it) }
}

internal class ProviderProperty(private val content: String) : ValuesProvider {

    private val prop: Properties = Properties().apply {
        load(StringReader(content))
    }

    override fun hasKey(key: String): Boolean {
        return prop.containsKey(key)
    }

    override fun getString(key: String): String {
        return prop.getProperty(key)!!
    }
}

