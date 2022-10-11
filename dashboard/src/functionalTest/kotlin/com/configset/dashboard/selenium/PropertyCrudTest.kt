package com.configset.dashboard.selenium

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selenide
import com.configset.dashboard.selenium.pages.SearchPage
import com.configset.dashboard.selenium.pages.UpdatePropertyPage
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.ReadPropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import io.mockk.slot
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PropertyCrudTest : SeleniumTest() {

    @BeforeEach
    fun beforeSearchPage() {
        authenticated()
    }

    @Test
    fun `should fill update form`() {
        // given
        val propertyReadRequestSlot = slot<ReadPropertyRequest>()
        mockConfigServiceExt.whenReadProperty {
            capture(propertyReadRequestSlot)
        }.answer(
            PropertyItem.newBuilder()
                .setApplicationName("sampleApp")
                .setPropertyName("Foo")
                .setPropertyValue("bar")
                .setHostName("sampleHost")
                .setVersion(2)
                .build()
        )

        // when
        Selenide.open("/update?applicationName=sampleApp&propertyName=Foo&hostName=sampleHost")

        // then
        UpdatePropertyPage.applicationNameInput.apply {
            shouldBe(Condition.visible)
            shouldHave(Condition.value("sampleApp"))
        }
        UpdatePropertyPage.hostNameInput.apply {
            shouldBe(Condition.visible)
            shouldHave(Condition.value("sampleHost"))
        }
        UpdatePropertyPage.propertyNameInput.apply {
            shouldBe(Condition.visible)
            shouldHave(Condition.value("Foo"))
        }
        UpdatePropertyPage.propertyValueInput.apply {
            shouldBe(Condition.visible)
            shouldHave(Condition.value("bar"))
        }
        propertyReadRequestSlot.captured.also {
            it.applicationName shouldBeEqualTo "sampleApp"
            it.propertyName shouldBeEqualTo "Foo"
            it.hostName shouldBeEqualTo "sampleHost"
        }
    }


    @Test
    fun `should update property and redirect back`() {
        // given
        mockConfigServiceExt.whenReadProperty { any() }
            .answer(
                PropertyItem.newBuilder()
                    .setApplicationName("sampleApp")
                    .setPropertyName("Foo")
                    .setPropertyValue("bar")
                    .setHostName("sampleHost")
                    .setVersion(2)
                    .build()
            )
        mockConfigServiceExt.whenUpdateProperty { any() }
            .answer(UpdatePropertyResponse.Type.OK)

        // when
        Selenide.open("/update?applicationName=sampleApp&propertyName=Foo&hostName=sampleHost")
        UpdatePropertyPage.propertyValueInput.value = "new value"
        UpdatePropertyPage.updateButton.click()

        // then
        SearchPage.searchButton.shouldBe(Condition.visible)
    }

    @Test
    fun `should show error on property update`() {
        // given
        mockConfigServiceExt.whenReadProperty { any() }
            .answer(
                PropertyItem.newBuilder()
                    .setApplicationName("sampleApp")
                    .setPropertyName("Foo")
                    .setPropertyValue("bar")
                    .setHostName("sampleHost")
                    .setVersion(2)
                    .build()
            )
        mockConfigServiceExt.whenUpdateProperty { any() }
            .answer(UpdatePropertyResponse.Type.UPDATE_CONFLICT)

        // when
        Selenide.open("/update?applicationName=sampleApp&propertyName=Foo&hostName=sampleHost")
        UpdatePropertyPage.propertyValueInput.value = "new value"
        UpdatePropertyPage.updateButton.click()

        // then
        UpdatePropertyPage.errorContainer.shouldBe(Condition.visible)
    }
}