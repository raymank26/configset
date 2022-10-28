package com.configset.dashboard

import arrow.core.Either
import org.junit.Assert

fun <L, R> Either<L, R>.expectLeft(): L {
    when (this) {
        is Either.Left -> return value
        is Either.Right -> {
            Assert.fail("Expected Left, but Right is given $value")
            error("Unreachable")
        }
    }
}

fun <L, R> Either<L, R>.expectRight(): R {
    when (this) {
        is Either.Right -> return value
        is Either.Left -> {
            Assert.fail("Expected Right, but Left is given $value")
            error("Unreachable")
        }
    }
}
