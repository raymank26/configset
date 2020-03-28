package com.letsconfig

import com.letsconfig.model.PropertiesChanges
import com.letsconfig.model.PropertiesObserver
import com.letsconfig.model.PropertyItem
import org.awaitility.Awaitility
import org.junit.Assert.assertEquals
import org.junit.Test

class PropertiesObservableImplTest {

    private val startProperties = PropertiesChanges(1, listOf(PropertyItem.Updated("app", "key", "value", 1)))
    private val controller = PropertiesObservableController(startProperties)

    @Test
    fun testStart() {
        val propertiesObserver = InMemoryPropertiesObserver(null)
        controller.addObserver(propertiesObserver)

        assertEquals(startProperties.items, propertiesObserver.propertiesChanges)
    }

    @Test
    fun testUpdate() {
        val propertiesObserver = InMemoryPropertiesObserver(null)
        controller.addObserver(propertiesObserver)

        val updateProperties = PropertiesChanges(3, listOf(
                PropertyItem.Deleted("app", "key", 2),
                PropertyItem.Updated("app2", "key2", "value2", 3)
        ))
        controller.updateProperties(updateProperties)

        Awaitility.await().untilAsserted {
            assertEquals(startProperties.items.plus(updateProperties.items), propertiesObserver.propertiesChanges)
        }
    }

    @Test
    fun testRemoveObserver() {
        val propertiesObserver = InMemoryPropertiesObserver(null)
        controller.addObserver(propertiesObserver)

        controller.removeObserver(propertiesObserver)

        controller.updateProperties(PropertiesChanges(3, listOf(
                PropertyItem.Updated("app", "key", "value", 2),
                PropertyItem.Updated("app2", "key2", "value2", 3)
        )))

        assertEquals(startProperties.items, propertiesObserver.propertiesChanges)
    }

    @Test
    fun expiredPush() {
        val propertiesObserver = InMemoryPropertiesObserver(null)
        controller.addObserver(propertiesObserver)

        val updateProperties4 = PropertiesChanges(4, listOf(
                PropertyItem.Updated("app", "key", "value", 3),
                PropertyItem.Updated("app2", "key2", "value2", 4)
        ))
        controller.updateProperties(updateProperties4)
        controller.updateProperties(PropertiesChanges(3, listOf(
                PropertyItem.Updated("app3", "key4", "value4", 3)
        )))

        Awaitility.await().untilAsserted {
            assertEquals(startProperties.items.plus(updateProperties4.items), propertiesObserver.propertiesChanges)
        }
    }
}

class PropertiesObservableController(startProperties: PropertiesChanges) {
    private val propertiesRepository: InMemoryPropertiesRepository = InMemoryPropertiesRepository(startProperties)
    private val propertiesObservable = PropertiesObservable(propertiesRepository, BlockingObservableQueue())

    init {
        propertiesObservable.start()
    }

    fun updateProperties(propertiesChanges: PropertiesChanges) {
        propertiesRepository.pushChanges(propertiesChanges)
    }

    fun addObserver(propertiesObserver: PropertiesObserver) {
        propertiesObservable.addObserver(propertiesObserver)
    }

    fun removeObserver(propertiesObserver: PropertiesObserver) {
        propertiesObservable.removeObserver(propertiesObserver)
    }
}