package com.configset.server.auth

import com.auth0.jwt.JWT
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.Test
import java.util.concurrent.TimeUnit

class OAuth2AuthenticatorTest {

    private val authenticator = run {
        val oauth2Api = mockk<OAuth2Api>()
        every { oauth2Api.getResource() } answers {
            ResourceInfo("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq83KBqrOTEp4OX2cj3JBnHE0nvbQJC4WjXbTnFztzlhT8gCU20WGeI0AuufdHk3H+s2Er2MrJEEp+5puVWfT+VrqHqVUorwCaBmNYpfR8bx9W3NYXm2RqI3Eu1px+yDwo0hqJwdJJoOCe6cyI4CyyC9boAUl8SnDznCPgqeRoLOJaMwrf19YzCO2EpeUtw+oUzaZOTaaAQEYEiP/QKySm6i2QIJuJF7f2crFsgq5dbeen3fg2no/ZSMEQUO9JjW75055UbB3CCGk3ol8QoQ9QjankgAt3d/9svpLCWAcQZYiWMnQ2Z/Tp6yyQW16lT8z0sooSwkMmnkTGE4RrrhAswIDAQAB")
        }
        val k = OAuth2Authenticator(oauth2Api) { algo ->
            JWT.require(algo).acceptExpiresAt(TimeUnit.DAYS.toMillis(356L * 100)).build()
        }
        k.init()
        k
    }

    @Test
    fun testUserInfo() {
        val userInfo = authenticator.getUserInfo(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJsRnQ5VW10cUx6YzdYdFJsRUljZlhRblF1OGJNSU5ZY0l3R2xlLWFnbjkwIn0.eyJleHAiOjE2Mjg3MjAzMDQsImlhdCI6MTYyODcyMDAwNCwiYXV0aF90aW1lIjoxNjI4NzE4NTg5LCJqdGkiOiJhZmZlZTU3ZS1mZTIzLTRiODQtYWVjZC05MmMzNmFiZDVlYmEiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwNzcvYXV0aC9yZWFsbXMvZGVtbyIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiIwMzVjZTY1Zi0wMTMyLTQ5NjktYWRhZC0wNzM2ODU1OTcxOWMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJkZW1vLWNsaWVudElkIiwibm9uY2UiOiJkOTE1ZTlhNC1mMzljLTQxZTUtOGI0NC1mNjZkZmNlMDAxMjgiLCJzZXNzaW9uX3N0YXRlIjoiZGIyMjY0MWYtOTdhNi00ZDdjLTkzYmMtNjU5NGQ1MThkNjI1IiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0OjgwODAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImFkbWluaXN0cmF0b3IiLCJvZmZsaW5lX2FjY2VzcyIsImRlZmF1bHQtcm9sZXMtZGVtbyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCIsInNpZCI6ImRiMjI2NDFmLTk3YTYtNGQ3Yy05M2JjLTY1OTRkNTE4ZDYyNSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJyYXltYW5rMjYifQ.SetodvC0Kk50ci1_MxQRol2fp1Qz7IjEVztRck_lUTsfPn-HOGulNz2gwuw06ka0qoAyLAE4zEc1fnCaONFCpvsXbZXoclRZKmEF93isQUvXpMgA_57q3jJcjhQ7EiAPFLFGRpGyPT8VitDajZ7rtqkOqu3ZMvN_M2QYC7dWl2ccCp259ToKie_15JxmOG-unYHsDqAV6gNR2yxnfvcGxEb1zdbRG9890w-PLDZDn6bjg55__7YGXxLnS9Ie8qBsYIpFAJnYvMhs06NqD57zwtlx7RL8fwAFFW1qt7Yb_iuAnaYiDhMZYrKa_047abrfo341X9-wk8ADgkXCUfrKuw")
        userInfo.userName `should be equal to` "raymank26"
        userInfo.roles `should contain` "administrator"
    }
}