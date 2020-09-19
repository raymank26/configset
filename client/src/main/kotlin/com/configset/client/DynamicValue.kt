package com.configset.client

data class DynamicValue<T, K>(val initial: T, val observable: Observable<K>)