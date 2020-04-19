import com.letsconfig.client.converter.StringConverter
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Rule
import org.junit.Test

class ClientTest {

    @Rule
    @JvmField
    val serverRule = ServerRule()

    @Test
    fun test() {
        val confProperty = serverRule.configuration.getConfProperty(APP_NAME, StringConverter())
        confProperty.getValue() shouldBeEqualTo null
    }
}