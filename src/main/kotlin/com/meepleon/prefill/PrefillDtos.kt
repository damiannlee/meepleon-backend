package com.meepleon.prefill

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class PrefillRequest(
    @field:NotBlank
    @field:Size(max = 2048)
    @field:Pattern(regexp = "^https?://.+", message = "must be an http(s) URL")
    val url: String,
)

/** Draft OG-derived values for the registration/report form — field names mirror EventSubmissionRequest. */
data class PrefillResponse(
    val title: String?,
    val coverImageUrl: String?,
    val description: String?,
    val originalUrl: String,
)
