package com.configset.client

data class DynamicValue<T>(val value: T, val observable: Observable<T>)
