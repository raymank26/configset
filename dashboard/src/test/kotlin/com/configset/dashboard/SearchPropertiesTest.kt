package com.configset.dashboard

import com.configset.sdk.proto.ShowPropertyItem
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class SearchPropertiesTest : BaseDashboardTest() {

    @Test
    fun testSearch() {
        mockConfigServiceExt.whenSearchProperties()
            .intercept { req -> req.applicationName shouldBeEqualTo "testApp" }
            .answer {
                listOf(ShowPropertyItem.newBuilder().build())
            }
        searchProperties(applicationName = "testApp").size shouldBeEqualTo 1
    }
}
