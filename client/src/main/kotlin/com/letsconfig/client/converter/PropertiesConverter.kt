package com.letsconfig.client.converter

import java.io.StringReader
import java.util.*

internal class ConverterProperties : Converter<Properties> {
    override fun convert(value: String): Properties {
        val res = Properties()
        res.load(StringReader(value))
        return res
    }
}
