package com.letsconfig.network

interface NetworkPropertyChange {
    val version: Long
    val property: List<NetworkProperty>
}
