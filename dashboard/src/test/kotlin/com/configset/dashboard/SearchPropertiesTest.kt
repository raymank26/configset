package com.configset.dashboard

import com.configset.dashboard.infra.BaseDashboardTest
import com.configset.dashboard.infra.expectRight
import com.configset.sdk.proto.ShowPropertyItem
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class SearchPropertiesTest : BaseDashboardTest() {

    @Test
    fun testSearch() {
        mockConfigServiceExt.whenSearchProperties()
            .answer { req ->
                req.applicationName shouldBeEqualTo "testApp"
                listOf(ShowPropertyItem.newBuilder()
                    .build())
            }
        dashboardClient.searchProperties(applicationName = "testApp")
            .expectRight()
            .size shouldBeEqualTo 1
    }
}
