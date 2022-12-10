package com.configset.dashboard.util

import arrow.core.Either

// An "arrow.core" library already contains "either.eager" method to chain Either type together. But it's build on
// coroutines which are harder to debug due to bloated stack trace. This version provides simpler implementation.
fun <K, V> binding(lambda: BindingContext.() -> Either<K, V>): Either<K, V> {
    return try {
        lambda.invoke(BindingContext())
    } catch (e: LeftException) {
        @Suppress("UNCHECKED_CAST")
        e.failedEither as Either<K, V>
    }
}

class BindingContext {
    fun <L, R> Either<L, R>.bind(): R = when (this) {
        is Either.Left -> throw LeftException(this)
        is Either.Right -> this.value
    }
}

private class LeftException(val failedEither: Either<*, *>) : Exception()
