package com.configset.client.converter

class MapConverter<K, V>(
    private val keyConverter: Converter<K>,
    private val valueConverter: Converter<V>,
    private val delimitersPair: Array<String> = arrayOf(";", "\n"),
    private val delimitersKeyValue: String = "="
) : Converter<Map<K, V>> {
    override fun convert(value: String): Map<K, V> {
        return value.splitToSequence(*delimitersPair)
            .mapNotNull { pair ->
                if (pair.isBlank()) {
                    return@mapNotNull null
                }
                val (key, v) = pair.split(delimitersKeyValue, limit = 2)
                keyConverter.convert(key) to valueConverter.convert(v)
            }
            .toMap()
    }
}
