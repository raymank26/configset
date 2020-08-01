package com.letsconfig.client.converter

class CollectionConverter<T, K : Collection<T>>(private val converter: Converter<T>,
                                                private val collectionBuilder: (Sequence<T>) -> K,
                                                private val delimiters: Array<String> = arrayOf(",")) : Converter<K> {

    override fun convert(value: String): K {
        if (value.isEmpty()) {
            return collectionBuilder.invoke(emptySequence())
        }
        val split: Sequence<T> = value
                .splitToSequence(*delimiters)
                .map {
                    converter.convert(it) ?: throw IllegalStateException("Value is null, str = $it")
                }
        return collectionBuilder.invoke(split)
    }
}
