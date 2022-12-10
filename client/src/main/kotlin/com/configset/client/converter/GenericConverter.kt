package com.configset.client.converter

class GenericConverter<T>(private val delegate: (String) -> T) : Converter<T> {
    override fun convert(value: String): T {
        return delegate.invoke(value)
    }
}
