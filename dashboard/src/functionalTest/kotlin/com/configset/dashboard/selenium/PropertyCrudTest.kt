package com.configset.dashboard.selenium

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selenide
import com.configset.dashboard.selenium.pages.SearchPage
import com.configset.dashboard.selenium.pages.UpdatePropertyPage
import com.configset.sdk.proto.DeletePropertyRequest
import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.ReadPropertyRequest
import com.configset.sdk.proto.SearchPropertiesRequest
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

    @Test
    fun `should delete property and redirect back`() {
        // given
        val request = SearchPropertiesRequest.newBuilder()
            .setApplicationName("Sample app")
            .setPropertyName("")
            .setHostName("")
            .setPropertyValue("")
            .build()
        val properties = listOf(
            PropertyItem.newBuilder()
                .setApplicationName("Sample app")
                .setPropertyName("Foo")
                .setPropertyValue("bar")
                .setHostName("sample host")
                .setVersion(2)
                .build(),
        )
        mockConfigServiceExt.whenSearchProperties {
            eq(request)
        }.answer(properties)

        val deletePropertyRequest = slot<DeletePropertyRequest>()
        mockConfigServiceExt.whenDeleteProperty {
            capture(deletePropertyRequest)
        }.answer(
            DeletePropertyResponse.newBuilder()
                .setType(DeletePropertyResponse.Type.OK)
                .build()
        )

        // when
        Selenide.open("/")
        SearchPage.applicationNameInput.value = "Sample app"
        SearchPage.searchButton.click()
        val rowElement = SearchPage.findSearchResultRow(properties[0].applicationName, properties[0].propertyName)
        rowElement.getExpandButton().click()
        val foundItem = rowElement.getPropertyItem("sample host", "bar")
        foundItem.getDeleteButton().click()
        Selenide.switchTo().alert().accept()

        // then
        SearchPage.searchButton.shouldBe(Condition.visible)
        deletePropertyRequest.captured.also {
            it.applicationName shouldBeEqualTo properties[0].applicationName
            it.hostName shouldBeEqualTo properties[0].hostName
            it.propertyName shouldBeEqualTo properties[0].propertyName
        }
    }

    @Test
    fun `should show error on property delete`() {
        // given
        val request = SearchPropertiesRequest.newBuilder()
            .setApplicationName("Sample app")
            .setPropertyName("")
            .setHostName("")
            .setPropertyValue("")
            .build()
        val properties = listOf(
            PropertyItem.newBuilder()
                .setApplicationName("Sample app")
                .setPropertyName("Foo")
                .setPropertyValue("bar")
                .setHostName("sample host")
                .setVersion(2)
                .build(),
        )
        mockConfigServiceExt.whenSearchProperties {
            eq(request)
        }.answer(properties)

        mockConfigServiceExt.whenDeleteProperty {
            any()
        }.answer(
            DeletePropertyResponse.newBuilder()
                .setType(DeletePropertyResponse.Type.DELETE_CONFLICT)
                .build()
        )

        // when
        Selenide.open("/")
        SearchPage.applicationNameInput.value = "Sample app"
        SearchPage.searchButton.click()
        val rowElement = SearchPage.findSearchResultRow(properties[0].applicationName, properties[0].propertyName)
        rowElement.getExpandButton().click()
        val foundItem = rowElement.getPropertyItem("sample host", "bar")
        foundItem.getDeleteButton().click()
        Selenide.switchTo().alert().accept()

        // then
        Selenide.switchTo().alert().text.shouldBeEqualTo("CONFLICT")
    }
}