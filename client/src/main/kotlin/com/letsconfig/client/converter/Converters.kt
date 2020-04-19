package com.letsconfig.client.converter

object Converters {
    val STRING = GenericConverter { str -> str }
    val LONG = GenericConverter { str -> str.toLong() }
    val INTEGER = GenericConverter { str -> str.toInt() }
    val CHAR = GenericConverter { str ->
        require(str.length == 1)
        str.first()
    }
}