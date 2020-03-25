package com.letsconfig.network

interface NetworkObserver {
    fun handleChanges(items: NetworkPropertyChange)
    fun getLastKnownVersion(): Long
}