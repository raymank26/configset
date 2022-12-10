package com.configset.client.converter

import java.io.StringReader
import java.util.Properties

internal class PropertiesConverter : Converter<Properties> {
    override fun convert(value: String): Properties {
        val res = Properties()
        res.load(StringReader(value))
        return res
    }
}
