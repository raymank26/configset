package com.configset.server.auth

import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.Test
import java.net.http.HttpClient
import java.net.http.HttpResponse

class OAuth2AuthenticatorTest {

    private val httpClient = mockk<HttpClient>()

    private val authenticator = run {
        OAuth2Authenticator(
            baseUrl = "http://localhost",
            timeoutMs = 2000,
            httpClient = httpClient
        )
    }

    @Test
    fun `should retrieve OpenID and parse user info`() {
        // given
        val response = mockk<HttpResponse<Any>>()
        every {
            response.statusCode()
        }.returns(200)

        every {
            httpClient.send<Any>(any(), any())
        }.returns(response)

        // when
        val userInfo = authenticator.getUserInfo(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJsRnQ5VW10cUx6YzdYdFJsRUljZlhRblF1OGJNSU5ZY0l3R2xlLWFnbjkwIn0.eyJleHAiOjE2Mjg3MjAzMDQsImlhdCI6MTYyODcyMDAwNCwiYXV0aF90aW1lIjoxNjI4NzE4NTg5LCJqdGkiOiJhZmZlZTU3ZS1mZTIzLTRiODQtYWVjZC05MmMzNmFiZDVlYmEiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwNzcvYXV0aC9yZWFsbXMvZGVtbyIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiIwMzVjZTY1Zi0wMTMyLTQ5NjktYWRhZC0wNzM2ODU1OTcxOWMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJkZW1vLWNsaWVudElkIiwibm9uY2UiOiJkOTE1ZTlhNC1mMzljLTQxZTUtOGI0NC1mNjZkZmNlMDAxMjgiLCJzZXNzaW9uX3N0YXRlIjoiZGIyMjY0MWYtOTdhNi00ZDdjLTkzYmMtNjU5NGQ1MThkNjI1IiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0OjgwODAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImFkbWluaXN0cmF0b3IiLCJvZmZsaW5lX2FjY2VzcyIsImRlZmF1bHQtcm9sZXMtZGVtbyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCIsInNpZCI6ImRiMjI2NDFmLTk3YTYtNGQ3Yy05M2JjLTY1OTRkNTE4ZDYyNSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJyYXltYW5rMjYifQ.SetodvC0Kk50ci1_MxQRol2fp1Qz7IjEVztRck_lUTsfPn-HOGulNz2gwuw06ka0qoAyLAE4zEc1fnCaONFCpvsXbZXoclRZKmEF93isQUvXpMgA_57q3jJcjhQ7EiAPFLFGRpGyPT8VitDajZ7rtqkOqu3ZMvN_M2QYC7dWl2ccCp259ToKie_15JxmOG-unYHsDqAV6gNR2yxnfvcGxEb1zdbRG9890w-PLDZDn6bjg55__7YGXxLnS9Ie8qBsYIpFAJnYvMhs06NqD57zwtlx7RL8fwAFFW1qt7Yb_iuAnaYiDhMZYrKa_047abrfo341X9-wk8ADgkXCUfrKuw")

        // then
        userInfo.userName `should be equal to` "raymank26"
        userInfo.roles `should contain` "administrator"
    }
}