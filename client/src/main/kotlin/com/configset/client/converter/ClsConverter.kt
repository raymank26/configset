package com.configset.client.converter

import com.configset.client.reflect.ClsRuntimeExplorer
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class ClsConverter<T : Any>(private val cls: Class<T>) : Converter<T> {

    override fun convert(value: String): T {
        val runtimeData = ClsRuntimeExplorer.getRuntimeData(cls.kotlin)
        if (runtimeData.values.find { it.returnInfo.nested }?.let { true } == true) {
            error("Nested ConfProperty return types are not supported")
        }

        val converter = MapConverter(Converters.STRING, Converters.STRING)
        val mapValues = converter.convert(value)
        val methodToValue: Map<Method, Any?> = runtimeData.mapValues { (_, methodInfo) ->
            val resolvedValue = mapValues[methodInfo.methodName] ?: methodInfo.defaultValue
            if (resolvedValue == null) {
                null
            } else {
                methodInfo.returnInfo.converter.convert(resolvedValue)
            }
        }

        return Proxy.newProxyInstance(this.javaClass.classLoader, arrayOf(cls)) { proxy, method, args ->
            if (methodToValue.contains(method)) {
                val returnValue = methodToValue[method]
                val methodInfo = runtimeData[method]!!
                methodInfo.validateReturnValue(returnValue)
                returnValue
            } else {
                method.invoke(proxy, args)
            }
        } as T
    }
}
