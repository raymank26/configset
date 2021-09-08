package com.configset.client

data class DynamicValue<T, K>(val value: T, val observable: Observable<K>)