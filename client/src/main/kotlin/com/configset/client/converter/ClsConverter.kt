package com.configset.client.converter

import com.configset.client.reflect.ClsRuntimeExplorer
import java.lang.reflect.Proxy

class ClsConverter<T>(private val cls: Class<T>) : Converter<T> {

    override fun convert(value: String): T {
        val runtimeData = ClsRuntimeExplorer.getRuntimeData(cls)
        if (runtimeData.values.find { it.returnInfo.nested }?.let { true } == true) {
            error("Nested ConfProperty return types are not supported")
        }

        val converter = MapConverter(Converters.STRING, Converters.STRING)
        val mapValues = converter.convert(value)

        return Proxy.newProxyInstance(this.javaClass.classLoader, arrayOf(cls)) { proxy, method, args ->
            val methodInfo = runtimeData[method]
                ?: return@newProxyInstance method.invoke(proxy, args)

            val propertyValue = mapValues[methodInfo.methodName]
            methodInfo.returnInfo.converter.convert(propertyValue ?: methodInfo.defaultValue)
        } as T
    }
}