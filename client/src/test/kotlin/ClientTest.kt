import com.letsconfig.client.ConfProperty
import com.letsconfig.client.converter.Converters
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.Rule
import org.junit.Test

class ClientTest {

    @Rule
    @JvmField
    val serverRule = ServerRule()

    @Test
    fun `test subscribe update, delete cycle`() {
        val propertyName = "configuration.property"
        val confProperty: ConfProperty<String?> = serverRule.configuration.getConfProperty(propertyName, Converters.STRING)

        confProperty.getValue() shouldBeEqualTo null

        serverRule.createApplication(APP_NAME)
        serverRule.createHost(HOSTNAME)

        val expectedValueAfterUpdate = "123"

        serverRule.updateProperty(APP_NAME, HOSTNAME, null, propertyName, expectedValueAfterUpdate)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo expectedValueAfterUpdate
        }

        serverRule.deleteProperty(APP_NAME, HOSTNAME, propertyName)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo null
        }
    }
}