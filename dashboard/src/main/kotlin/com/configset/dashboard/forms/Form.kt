package com.configset.dashboard.forms

import arrow.core.Either
import arrow.core.left
import arrow.core.right

class Form(
    private val fields: List<FormField>,
    @Suppress("MemberVisibilityCanBePrivate")
    val commonError: String? = null
) {

    private val fieldsMap: Map<String, FormField> = fields
        .groupBy { it.name }
        .mapValues { it.value[0] }

    val hasError = commonError != null || fields.find { it.error != null } != null

    fun withDefaultValues(valuesMap: Map<String, String>): Form {
        return fields.map {
            val defaultValue = valuesMap[it.name]
            if (defaultValue != null) {
                it.copy(value = defaultValue)
            } else {
                it
            }
        }.let {
            Form(it, commonError)
        }
    }

    fun hasField(name: String): Boolean = fieldsMap.containsKey(name)

    fun getField(name: String): FormField = fieldsMap[name]!!

    fun withCommonError(commonError: String): Form {
        return Form(fields, commonError)
    }

    fun withFieldError(fieldName: String, error: String): Form {
        return Form(fields.map { if (it.name == fieldName) it.copy(error = error) else it }, commonError)
    }

    fun withReadonlyFields(readonlyFields: Set<String>): Form {
        return Form(fields.map { if (readonlyFields.contains(it.name)) it.copy(readonly = true) else it }, commonError)
    }

    fun performValidation(formValues: Map<String, List<String>>): Either<InvalidForm, ValidForm> {
        @Suppress("UNCHECKED_CAST")
        return doValidation(formValues
            .mapValues { it.value.getOrNull(0) }
            .filterValues { it != null } as Map<String, String>)
    }

    private fun doValidation(formValues: Map<String, String>): Either<InvalidForm, ValidForm> {
        val validatedFields = fields.map {
            val value = formValues[it.name]
            if (!it.required && value.isNullOrBlank()) {
                it
            } else if (it.required && value == null) {
                it.copy(error = "Required")
            } else if (value == null) {
                it
            } else when (val validationRes = it.validation.validate(value)) {
                is FormError.Ok -> it.copy(value = value)
                is FormError.CustomError -> it.copy(error = validationRes.text, value = value)
            }
        }
        val res = Form(validatedFields, commonError)
        return if (validatedFields.find { it.error != null } != null || commonError != null) {
            InvalidForm(res).left()
        } else {
            ValidForm(res).right()
        }
    }
}

sealed class FormError {
    object Ok : FormError()
    data class CustomError(val text: String) : FormError()
}

data class FormField(
    val label: String,
    val required: Boolean,
    val name: String,
    val validation: FormFieldValidator = noValidation,
    val readonly: Boolean = false,
    val inlineLabel: Boolean = false,
    val error: String? = null,
    val value: String? = null,
)

interface FormFieldValidator {
    fun validate(value: String): FormError

    companion object {

        val NOT_BLANK = object : FormFieldValidator {
            override fun validate(value: String): FormError {
                return if (value.isBlank()) FormError.CustomError("Value is blank") else FormError.Ok
            }
        }
        val IS_LONG = object : FormFieldValidator {
            override fun validate(value: String): FormError {
                return if (value.toLongOrNull() == null) FormError.CustomError("Value is not Long") else FormError.Ok
            }
        }
    }
}

val noValidation = object : FormFieldValidator {
    override fun validate(value: String): FormError {
        return FormError.Ok
    }
}

fun FormFieldValidator.and(other: FormFieldValidator): FormFieldValidator {
    val that = this
    return object : FormFieldValidator {
        override fun validate(value: String): FormError {
            val res1 = that.validate(value)
            if (res1 is FormError.Ok) {
                return other.validate(value)
            }
            return res1
        }
    }
}

data class InvalidForm(val form: Form)
data class ValidForm(val form: Form)


