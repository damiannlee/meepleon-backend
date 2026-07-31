package com.meepleon.prefill

import org.springframework.boot.context.properties.ConfigurationProperties

/** Tunables for the URL prefill fetch (ADR-0009, ADR-0006 2026-07-23 revision). */
@ConfigurationProperties(prefix = "meepleon.prefill")
class PrefillProperties(
    val connectTimeoutMs: Long = 3000,
    val readTimeoutMs: Long = 5000,
    val maxRedirects: Int = 3,
    val maxResponseBytes: Long = 2_000_000,
    /** Empty = unrestricted. Suffix-matched against the request host if non-empty. */
    val allowedHosts: List<String> = emptyList(),
    val rateLimitMaxRequestsPerWindow: Int = 20,
    val rateLimitWindowHours: Long = 1,
)
