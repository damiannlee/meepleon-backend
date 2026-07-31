package com.meepleon.prefill

import com.meepleon.common.BadRequestException
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private const val USER_AGENT = "Meepleon-Prefill/1.0 (+https://meepleon.app; single-URL metadata fetch on user request)"
private const val READ_CHUNK_BYTES = 8192

/**
 * Single-fetch, SSRF-safe HTML fetcher for the URL prefill feature (ADR-0009).
 * Redirects are followed manually (never via HttpClient's built-in follower) so each hop
 * gets re-validated by SsrfGuard, and no Accept-Encoding is sent — the JDK client can't
 * transparently decode a compressed body anyway, and skipping it avoids ever needing a
 * decompression step at all, which structurally rules out a decompression-bomb response.
 */
@Component
class UrlFetcher(
    private val properties: PrefillProperties,
    private val ssrfGuard: SsrfGuard,
) {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    fun fetch(url: String): String {
        var currentUri = parseUri(url)
        for (attempt in 0..properties.maxRedirects) {
            ssrfGuard.validate(currentUri)
            val response = send(currentUri)
            if (isRedirect(response.statusCode())) {
                currentUri = resolveRedirect(currentUri, response)
                continue
            }
            if (response.statusCode() != 200) {
                response.body().close()
                throw BadRequestException("Fetch failed with status ${response.statusCode()}")
            }
            return readBounded(response.body())
        }
        throw BadRequestException("Too many redirects")
    }

    private fun parseUri(url: String): URI = try {
        URI.create(url)
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("Invalid URL")
    }

    private fun send(uri: URI): HttpResponse<InputStream> {
        val request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofMillis(properties.readTimeoutMs))
            .header("User-Agent", USER_AGENT)
            .GET()
            .build()
        return try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        } catch (e: IOException) {
            throw BadRequestException("Could not fetch URL: ${e.message}")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw BadRequestException("Fetch was interrupted")
        }
    }

    private fun isRedirect(statusCode: Int): Boolean = statusCode in 300..399

    private fun resolveRedirect(current: URI, response: HttpResponse<InputStream>): URI {
        response.body().close()
        val location = response.headers().firstValue("Location")
            .orElseThrow { BadRequestException("Redirect without a Location header") }
        return current.resolve(location)
    }

    private fun readBounded(body: InputStream): String = body.use { stream ->
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(READ_CHUNK_BYTES)
        var total = 0L
        while (true) {
            val read = stream.read(chunk)
            if (read == -1) break
            total += read
            if (total > properties.maxResponseBytes) {
                throw BadRequestException("Response exceeded the size limit")
            }
            buffer.write(chunk, 0, read)
        }
        buffer.toString(Charsets.UTF_8)
    }
}
