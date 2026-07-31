package com.meepleon.prefill

import com.meepleon.common.BadRequestException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.net.URI

class SsrfGuardTest {

    private val guard = SsrfGuard(PrefillProperties())

    @Test
    fun `allows a public IP`() {
        guard.validate(URI.create("http://8.8.8.8/"))
    }

    @Test
    fun `blocks loopback`() {
        assertThrows(BadRequestException::class.java) {
            guard.validate(URI.create("http://127.0.0.1/"))
        }
    }

    @Test
    fun `blocks RFC1918 private ranges`() {
        listOf("http://10.0.0.5/", "http://172.16.0.5/", "http://192.168.1.5/").forEach { url ->
            assertThrows(BadRequestException::class.java) {
                guard.validate(URI.create(url))
            }
        }
    }

    @Test
    fun `blocks the cloud metadata endpoint`() {
        assertThrows(BadRequestException::class.java) {
            guard.validate(URI.create("http://169.254.169.254/"))
        }
    }

    @Test
    fun `rejects non-http schemes`() {
        assertThrows(BadRequestException::class.java) {
            guard.validate(URI.create("ftp://example.com/"))
        }
    }

    @Test
    fun `enforces an allowlist when configured`() {
        val scopedGuard = SsrfGuard(PrefillProperties(allowedHosts = listOf("tumblbug.com")))
        assertThrows(BadRequestException::class.java) {
            scopedGuard.validate(URI.create("http://8.8.8.8/"))
        }
    }
}
