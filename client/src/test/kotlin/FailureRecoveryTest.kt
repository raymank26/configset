import com.letsconfig.client.ConfProperty
import com.letsconfig.client.converter.Converters
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val DEFAULT_VALUE = 1L

class FailureRecoveryTest {

    @Rule
    @JvmField
    val serverRule = ServerRule()

    private val propertyName = "property.conf"
    private lateinit var confProperty: ConfProperty<Long>

    @Before
    fun before() {
        serverRule.createApplication(APP_NAME)
        serverRule.createHost(HOSTNAME)

        confProperty = serverRule.configuration.getConfProperty(propertyName, Converters.LONG, DEFAULT_VALUE)
    }

    @Test
    fun `test wrong value use default`() {
        serverRule.updateProperty(APP_NAME, HOSTNAME, null, propertyName, "123")
        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo 123L
        }

        serverRule.updateProperty(APP_NAME, HOSTNAME, 1, propertyName, "10.23") // double is not long => default value

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo DEFAULT_VALUE
        }
    }

    @Test
    fun `test failed listener`() {
        var capturedValue: Long? = null
        val stringValue = "1023"
        serverRule.updateProperty(APP_NAME, HOSTNAME, 1, propertyName, stringValue)
        confProperty.subscribe { value ->
            capturedValue = value
            throw RuntimeException("Unable to process value")
        }
        Awaitility.await().untilAsserted {
            capturedValue shouldBeEqualTo stringValue.toLong()
        }
    }
}