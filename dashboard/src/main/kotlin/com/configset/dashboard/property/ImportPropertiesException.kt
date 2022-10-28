package com.configset.dashboard.property

data class ImportPropertiesException(val type: ImportErrorType) : Exception()

enum class ImportErrorType {
    ILLEGAL_FORMAT
}

fun ImportErrorType.throwException() = ImportPropertiesException(this)
