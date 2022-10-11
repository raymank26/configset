package com.configset.dashboard.api

import com.configset.dashboard.FunctionalTest
import com.configset.dashboard.expectRight
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.SearchPropertiesRequest
import io.mockk.slot
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SearchPropertiesTest : FunctionalTest() {

    @Test
    fun testSearch() {
        // given
        val searchPropertiesRequest = slot<SearchPropertiesRequest>()
        mockConfigServiceExt.whenSearchProperties {
            capture(searchPropertiesRequest)
        }.answer(
            listOf(
                PropertyItem
                    .newBuilder()
                    .setHostName("srvd1")
                    .setApplicationName("testApp")
                    .setPropertyName("foo")
                    .setPropertyValue("bar")
                    .build()
            )
        )

        // when
        val result = dashboardClient.searchProperties(applicationName = "testApp")

        // then
        result.expectRight().size shouldBeEqualTo 1
        searchPropertiesRequest.captured.also {
            it.applicationName shouldBeEqualTo "testApp"
        }
    }
}
