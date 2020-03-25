package com.letsconfig

import com.letsconfig.model.PropertiesChanges
import com.letsconfig.model.PropertiesObserver
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PropertiesObservableImpl(
        private val propertiesRepository: PropertiesRepository,
        private val observableQueue: ObservableQueue<PropertiesChanges>
) : PropertiesObservable {

    private val observers: MutableSet<PropertiesObserver> = mutableSetOf()
    private val observersLock = ReentrantLock()

    @Volatile
    private var lastKnownVersion: Long? = null

    fun start() {
        propertiesRepository.subscribe {
            observableQueue.put(it)
        }
        updateProperties(propertiesRepository.getUpdatedProperties(null))
        observableQueue.subscribe {
            updateProperties(it)
        }
    }

    override fun addObserver(observer: PropertiesObserver) {
        observer.handleChanges(propertiesRepository.getUpdatedProperties(observer.lastKnownVersion()))
        observersLock.withLock {
            observers.add(observer)
        }
    }

    private fun updateProperties(updatedProperties: PropertiesChanges) {
        if (lastKnownVersion != null && updatedProperties.lastVersion <= lastKnownVersion!!) {
            return
        }
        if (updatedProperties.items.isEmpty()) {
            require(lastKnownVersion == updatedProperties.lastVersion)
            return
        }
        observersLock.withLock {
            for (observer in observers) {
                observer.handleChanges(updatedProperties)
            }
        }
        lastKnownVersion = updatedProperties.lastVersion
    }

    override fun removeObserver(observer: PropertiesObserver) {
        observersLock.withLock {
            observers.remove(observer)
        }
    }
}