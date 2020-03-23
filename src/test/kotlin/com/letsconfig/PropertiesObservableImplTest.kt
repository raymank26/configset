package com.letsconfig

import org.junit.Assert.assertEquals
import org.junit.Test

class PropertiesObservableImplTest {

    private val startProperties = PropertiesChanges(1, listOf(PropertyItem.Updated("app", "key", "value")))
    private val controller = PropertiesObservableController(startProperties)

    @Test
    fun testStart() {
        val propertiesObserver = InMemoryPropertiesObserver(null)
        controller.addObserver(propertiesObserver)

        assertEquals(startProperties, propertiesObserver.propertiesChanges)
    }

    @Test
    fun testUpdate() {
        val propertiesObserver = InMemoryPropertiesObserver(null)
        controller.addObserver(propertiesObserver)

        val updateProperties = PropertiesChanges(2, listOf(
                PropertyItem.Deleted("app", "key"),
                PropertyItem.Updated("app2", "key2", "value2")
        ))
        controller.updateProperties(updateProperties)

        assertEquals(updateProperties, propertiesObserver.propertiesChanges)
    }

    @Test
    fun testRemoveObserver() {
        val propertiesObserver = InMemoryPropertiesObserver(null)
        controller.addObserver(propertiesObserver)

        controller.removeObserver(propertiesObserver)

        controller.updateProperties(PropertiesChanges(2, listOf(
                PropertyItem.Updated("app", "key", "value"),
                PropertyItem.Updated("app2", "key2", "value2")
        )))

        assertEquals(startProperties, propertiesObserver.propertiesChanges)
    }
}

class PropertiesObservableController(startProperties: PropertiesChanges) {
    private val propertiesRepository: InMemoryPropertiesRepository = InMemoryPropertiesRepository(startProperties)
    private val scheduler: FakeScheduler = FakeScheduler()
    private val propertiesObservable = PropertiesObservableImpl(propertiesRepository, scheduler, 5)

    init {
        propertiesObservable.start()
    }

    fun updateProperties(propertiesChanges: PropertiesChanges) {
        propertiesRepository.propertiesChanges = propertiesChanges
        scheduler.fire()
    }

    fun updateProperties(fromVersion: Long?, toVersion: Long, propertiesChanges: PropertiesChanges) {
        propertiesRepository.putPropertiesChanges(fromVersion, toVersion, propertiesChanges)
    }

    fun addObserver(propertiesObserver: PropertiesObserver) {
        propertiesObservable.addObserver(propertiesObserver)
    }

    fun removeObserver(propertiesObserver: PropertiesObserver) {
        propertiesObservable.removeObserver(propertiesObserver)
    }
}