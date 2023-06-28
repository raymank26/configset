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
        val methodToValue = runtimeData.mapValues { (_, methodInfo) ->
            methodInfo.returnInfo.converter.convert(mapValues[methodInfo.methodName] ?: methodInfo.defaultValue)
        }

        return Proxy.newProxyInstance(this.javaClass.classLoader, arrayOf(cls)) { proxy, method, args ->
            methodToValue[method] ?: method.invoke(proxy, args)
        } as T
    }
}
