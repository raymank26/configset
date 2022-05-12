package com.configset.dashboard.util

sealed class Outcome<T, R> {
    data class Success<T, R>(val value: T) : Outcome<T, R>()
    data class Error<T, R>(val error: R) : Outcome<T, R>()

    companion object {
        fun <R> success(): Outcome<Unit, R> = Success(Unit)
        fun <T, R> success(value: T): Outcome<T, R> = Success(value)
        fun <T, R> error(error: R): Outcome<T, R> = Error(error)
    }

    fun orElseThrow(supplier: (R) -> Exception): T {
        return when (this) {
            is Error -> throw supplier.invoke(this.error)
            is Success -> this.value
        }
    }

    fun isSuccess() = this is Success

    fun isError() = this is Error

    fun <K> mapError(supplier: (R) -> K): Outcome<T, K> {
        return when (this) {
            is Error -> error(supplier.invoke(error))
            is Success -> success(value)
        }
    }
}