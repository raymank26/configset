package com.letsconfig.client

data class DynamicValue<T, K>(val initial: T, val observable: Observable<K>)