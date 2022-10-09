package com.configset.dashboard.headless

import com.configset.dashboard.FunctionalTest
import com.configset.dashboard.expectRight
import com.configset.sdk.proto.SearchPropertiesRequest
import com.configset.sdk.proto.ShowPropertyItem
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
                ShowPropertyItem
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
