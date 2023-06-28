package com.configset.client.reflect

import com.configset.client.ConfProperty
import com.configset.client.converter.Converter
import com.configset.client.converter.Converters
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal object ClsRuntimeExplorer {

    fun getRuntimeData(cls: Class<*>): Map<Method, MethodInfo> {
        require(cls.isInterface)
        return cls.methods
            .map { method ->
                if (!method.isDefault && method.getAnnotation(PropertyInfo::class.java) == null) {
                    error("Non-default method $method without @PropertyInfo annotation")
                }
                method
            }
            .associateWith {
                require(it.parameterCount == 0)
                val methodName = getMethodName(it)
                if (it.returnType == Unit::class.java) {
                    error("Method $it doesn't have return value")
                }
                val annotation = it.getAnnotation(PropertyInfo::class.java)
                MethodInfo(
                    getMethodReturnInfo(methodName, it.genericReturnType, it.returnType, annotation),
                    methodName,
                    annotation.defaultValue
                )
            }
    }

    private fun getMethodName(it: Method): String {
        val methodName = it.getAnnotation(PropertyInfo::class.java)
            .name
            .let { specifiedName ->
                if (specifiedName == "") {
                    if (it.name.startsWith("get")) {
                        it.name.drop(3).replaceFirstChar { ch -> ch.lowercase() }
                    } else {
                        it.name
                    }
                } else {
                    specifiedName
                }
            }
        return methodName
    }

    private fun getMethodReturnInfo(
        methodName: String,
        genericType: Type,
        returnType: Class<*>,
        annotation: PropertyInfo,
    ): MethodReturnInfo {
        var nested = false
        var realReturnType = genericType
        if (returnType == ConfProperty::class.java) {
            realReturnType = (genericType as ParameterizedType).actualTypeArguments[0]
            nested = true
        }
        val converter: Converter<*> = when (realReturnType) {
            String::class.java -> Converters.STRING
            Int::class.java -> Converters.INTEGER
            Integer::class.java -> Converters.INTEGER
            Long::class.java -> Converters.LONG
            Boolean::class.java -> Converters.BOOLEAN
            Double::class.java -> Converters.DOUBLE
            else -> if (annotation.converter != Converter::class) {
                annotation.converter.java.getDeclaredConstructor().apply {
                    isAccessible = true
                }.newInstance() as Converter<*>
            } else {
                error("Converter not found for methodName = $methodName")
            }
        }
        return MethodReturnInfo(converter, nested)
    }
}

internal data class MethodInfo(
    val returnInfo: MethodReturnInfo,
    val methodName: String,
    val defaultValue: String,
)

internal data class MethodReturnInfo(
    val converter: Converter<*>,
    val nested: Boolean,
)
