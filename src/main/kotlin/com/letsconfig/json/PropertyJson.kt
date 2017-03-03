package com.letsconfig.json

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author anton.ermak
 *         Date: 03.03.17.
 */
data class PropertyJson(@field:JsonProperty val value: String, @field:JsonProperty val updated_datetime: Long)
