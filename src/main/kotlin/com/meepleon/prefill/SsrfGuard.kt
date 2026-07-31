package com.meepleon.prefill

import com.meepleon.common.BadRequestException
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException

private val ALLOWED_SCHEMES = setOf("http", "https")

/**
 * Blocks the URL prefill fetcher (ADR-0009) from reaching internal/private network targets.
 * Called once per hop so redirects can't smuggle a request past the check (ADR-0006 2026-07-23 revision).
 */
@Component
class SsrfGuard(
    private val properties: PrefillProperties,
) {
    fun validate(uri: URI) {
        val scheme = uri.scheme?.lowercase()
        if (scheme !in ALLOWED_SCHEMES) {
            throw BadRequestException("Only http/https URLs are supported")
        }
        val host = uri.host ?: throw BadRequestException("URL must include a host")
        if (!isAllowedHost(host)) {
            throw BadRequestException("Host is not allowed: $host")
        }
        resolveAddresses(host).forEach { address ->
            if (isBlockedAddress(address)) {
                throw BadRequestException("Host resolves to a blocked address")
            }
        }
    }

    private fun isAllowedHost(host: String): Boolean {
        val allowedHosts = properties.allowedHosts
        if (allowedHosts.isEmpty()) return true
        return allowedHosts.any { allowed ->
            host.equals(allowed, ignoreCase = true) || host.endsWith(".$allowed", ignoreCase = true)
        }
    }

    private fun resolveAddresses(host: String): Array<InetAddress> = try {
        InetAddress.getAllByName(host)
    } catch (e: UnknownHostException) {
        throw BadRequestException("Could not resolve host: $host")
    }

    // isLinkLocalAddress covers 169.254.0.0/16, which includes the cloud metadata endpoint (169.254.169.254).
    // isSiteLocalAddress covers the RFC1918 private ranges.
    private fun isBlockedAddress(address: InetAddress): Boolean =
        address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress ||
            address.isAnyLocalAddress
}
