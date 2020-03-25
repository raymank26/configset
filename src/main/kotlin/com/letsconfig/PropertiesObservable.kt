package com.letsconfig

import com.letsconfig.model.PropertiesObserver

interface PropertiesObservable {
    fun addObserver(observer: PropertiesObserver)
    fun removeObserver(observer: PropertiesObserver)
}