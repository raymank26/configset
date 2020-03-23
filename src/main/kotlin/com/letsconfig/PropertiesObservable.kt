package com.letsconfig

interface PropertiesObservable {
    fun addObserver(observer: PropertiesObserver)
    fun removeObserver(observer: PropertiesObserver)
}