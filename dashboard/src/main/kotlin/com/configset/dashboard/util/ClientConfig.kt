package com.configset.dashboard.util

import com.fasterxml.jackson.annotation.JsonProperty

data class ClientConfig(
    @JsonProperty("keycloackUrl")
    val keycloackUrl: String,
    @JsonProperty("keycloackRealm")
    val keycloackRealm: String,
    @JsonProperty("keycloackClientId")
    val keycloackClientId: String

)
