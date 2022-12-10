package com.configset.client.converter

class EnumConverter<T : Enum<T>>(private val enumClazz: Class<T>) : Converter<T> {

    override fun convert(value: String): T {
        return java.lang.Enum.valueOf(enumClazz, value)
    }
}
