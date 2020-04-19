import com.letsconfig.client.ConfProperty
import com.letsconfig.client.converter.StringConverter
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.Rule
import org.junit.Test

class ClientTest {

    @Rule
    @JvmField
    val serverRule = ServerRule()

    @Test
    fun test() {
        val propertyName = "configuration.property"
        val confProperty: ConfProperty<String?> = serverRule.configuration.getConfProperty(propertyName, StringConverter())

        confProperty.getValue() shouldBeEqualTo null

        serverRule.createApplication(APP_NAME)
        serverRule.createHost(HOSTNAME)

        val expectedValue = "123"

        serverRule.updateProperty(APP_NAME, HOSTNAME, null, propertyName, expectedValue)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo expectedValue
        }

        serverRule.deleteProperty(APP_NAME, HOSTNAME, propertyName)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo null
        }
    }
}