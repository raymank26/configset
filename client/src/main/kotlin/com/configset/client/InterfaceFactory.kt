package com.configset.client

import com.configset.client.converter.Converters
import com.configset.client.converter.MapConverter
import com.configset.client.reflect.ClsRuntimeExplorer
import com.configset.client.reflect.MethodInfo
import com.configset.client.repository.property.ConfPropertyRepository
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class InterfaceFactory(private val propertyResolver: PropertyFullResolver) {

    private val stringMapConverter = MapConverter(Converters.STRING, Converters.STRING)

    fun <T : Any> getInterfaceConfProperty(confProperty: ConfProperty<String?>, cls: Class<T>): T {
        val runtimeData = ClsRuntimeExplorer.getRuntimeData(cls.kotlin)
        val repository = ConfPropertyRepository(transform(confProperty, runtimeData))
        val configurationSnapshot = repository.subscribeToProperties("app")
        val registry = ApplicationRegistry(configurationSnapshot, propertyResolver)

        return Proxy.newProxyInstance(this.javaClass.classLoader, arrayOf(cls)) { proxy, method, args ->

            val methodInfo = runtimeData[method]
                ?: return@newProxyInstance method.invoke(proxy, args)

            return@newProxyInstance if (methodInfo.returnInfo.nested) {
                registry.getConfProperty(methodInfo.methodName, methodInfo.returnInfo.converter)
            } else {
                val value = registry.getConfProperty(methodInfo.methodName, methodInfo.returnInfo.converter).getValue()
                methodInfo.validateReturnValue(value)
                value
            }
        } as T
    }

    private fun transform(
        confProperty: ConfProperty<String?>,
        runtimeData: Map<Method, MethodInfo>,
    ): ConfProperty<List<PropertyItem>> {
        val allValues = mutableMapOf<String, PropertyItem>()
        runtimeData.forEach { (_, methodInfo) ->
            allValues[methodInfo.methodName] =
                PropertyItem("app", methodInfo.methodName, 1L, methodInfo.defaultValue)
        }

        return confProperty.map {
            if (it != null) {
                stringMapConverter.convert(it).forEach { (key, value) ->
                    allValues[key] = PropertyItem("app", key, 1L, value)
                }
            }
            allValues.values.toList()
        }
    }
}
