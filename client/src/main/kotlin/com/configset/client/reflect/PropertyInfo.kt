package com.configset.client.reflect

import com.configset.client.converter.Converter
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class PropertyInfo(
    val name: String = "",
    val converter: KClass<out Converter<*>> = Converter::class,
    val defaultValue: String = "",
)