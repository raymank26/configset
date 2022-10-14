package com.configset.sdk.auth

import java.time.Instant

interface UserInfo {
    val accessToken: String
    val userName: String
    val roles: Set<String>

    fun accessTokenExpired(instant: Instant): Boolean
}