package com.letsconfig.client.converter

class MapConverter<K, V>(
        private val keyConverter: Converter<K>,
        private val valueConverter: Converter<V>,
        private val delimitersPair: Array<String> = arrayOf(";", "\n"),
        private val delimitersKeyValue: String = "="
) : Converter<Map<K, V>> {
    override fun convert(value: String): Map<K, V> {
        return value.splitToSequence(*delimitersPair)
                .map { pair ->
                    val (key, value_) = pair.split(delimitersKeyValue)
                    keyConverter.convert(key) to valueConverter.convert(value_)
                }
                .toMap()
    }
}
