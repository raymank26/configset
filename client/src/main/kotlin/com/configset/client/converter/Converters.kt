package com.configset.client.converter

import java.util.Properties

object Converters {
    val STRING = GenericConverter { it }
    val LONG = GenericConverter { it.toLong() }
    val INTEGER = GenericConverter { it.toInt() }
    val BOOLEAN = GenericConverter { it.toBooleanStrict() }
    val CHAR = GenericConverter { str ->
        require(str.length == 1)
        str.first()
    }
    val BYTE = GenericConverter { str -> str.toByte() }
    val DOUBLE = GenericConverter { it.toDouble() }
    val SHORT = GenericConverter { it.toShort() }
    val PROPERTIES: Converter<Properties> = PropertiesConverter()
    val LIST_STRING: Converter<List<String>> = listConverter(STRING)
    val LIST_LONG: Converter<List<Long>> = listConverter(LONG)
}

fun <T> listConverter(itemConverter: Converter<T>, delimiters: Array<String> = arrayOf(",")): Converter<List<T>> {
    return CollectionConverter(itemConverter, { it.toList() }, delimiters)
}

fun <T, P> Converter<T>.map(mapper: (T) -> P): Converter<P> {
    val that = this
    return object : Converter<P> {
        override fun convert(value: String): P {
            return mapper.invoke(that.convert(value))
        }
    }
}
