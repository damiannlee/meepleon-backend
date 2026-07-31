package com.meepleon.prefill

import com.meepleon.common.BadRequestException
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.net.InetSocketAddress

/** SsrfGuard is mocked out here (own tests in SsrfGuardTest) so these can exercise UrlFetcher
 *  against a real local server without loopback getting blocked by the guard it depends on. */
class UrlFetcherTest {

    private val server: HttpServer = HttpServer.create(InetSocketAddress("localhost", 0), 0).apply { start() }
    private val baseUrl = "http://localhost:${server.address.port}"
    private val ssrfGuard: SsrfGuard = mock()

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun `returns the body on a 200 response`() {
        server.createContext("/ok") { exchange ->
            val body = "<meta property=\"og:title\" content=\"hi\">".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }

        val result = UrlFetcher(PrefillProperties(), ssrfGuard).fetch("$baseUrl/ok")

        assertEquals(true, result.contains("og:title"))
    }

    @Test
    fun `follows a redirect chain`() {
        server.createContext("/start") { exchange ->
            exchange.responseHeaders.add("Location", "$baseUrl/end")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/end") { exchange ->
            val body = "ok".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }

        val result = UrlFetcher(PrefillProperties(), ssrfGuard).fetch("$baseUrl/start")

        assertEquals("ok", result)
    }

    @Test
    fun `gives up after exceeding the redirect limit`() {
        server.createContext("/loop") { exchange ->
            exchange.responseHeaders.add("Location", "$baseUrl/loop")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }

        assertThrows(BadRequestException::class.java) {
            UrlFetcher(PrefillProperties(maxRedirects = 2), ssrfGuard).fetch("$baseUrl/loop")
        }
    }

    @Test
    fun `fails when the response exceeds the size cap`() {
        server.createContext("/big") { exchange ->
            val body = ByteArray(2000)
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }

        assertThrows(BadRequestException::class.java) {
            UrlFetcher(PrefillProperties(maxResponseBytes = 1000), ssrfGuard).fetch("$baseUrl/big")
        }
    }

    @Test
    fun `fails on a non-200 status`() {
        server.createContext("/missing") { exchange ->
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }

        assertThrows(BadRequestException::class.java) {
            UrlFetcher(PrefillProperties(), ssrfGuard).fetch("$baseUrl/missing")
        }
    }

    @Test
    fun `fails when the response takes too long`() {
        server.createContext("/slow") { exchange ->
            Thread.sleep(500)
            val body = "late".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }

        assertThrows(BadRequestException::class.java) {
            UrlFetcher(PrefillProperties(readTimeoutMs = 200), ssrfGuard).fetch("$baseUrl/slow")
        }
    }
}
