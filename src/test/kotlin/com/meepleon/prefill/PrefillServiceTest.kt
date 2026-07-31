package com.meepleon.prefill

import com.meepleon.common.BadRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PrefillServiceTest {

    private val rateLimiter: PrefillRateLimiter = mock()
    private val urlFetcher: UrlFetcher = mock()
    private val ogMetadataExtractor: OgMetadataExtractor = mock()
    private val service = PrefillService(rateLimiter, urlFetcher, ogMetadataExtractor)

    @Test
    fun `assembles a response from extracted OG metadata`() {
        whenever(rateLimiter.tryConsume("203.0.113.10")).thenReturn(true)
        whenever(urlFetcher.fetch("https://tumblbug.com/x")).thenReturn("<html/>")
        whenever(ogMetadataExtractor.extract("<html/>")).thenReturn(
            OgMetadata(title = "제목", imageUrl = "https://img/1.jpg", description = "설명", url = "https://tumblbug.com/x"),
        )

        val response = service.prefill(PrefillRequest("https://tumblbug.com/x"), "203.0.113.10")

        assertEquals("제목", response.title)
        assertEquals("https://img/1.jpg", response.coverImageUrl)
        assertEquals("설명", response.description)
        assertEquals("https://tumblbug.com/x", response.originalUrl)
    }

    @Test
    fun `falls back to the requested URL when og-url is absent`() {
        whenever(rateLimiter.tryConsume("203.0.113.10")).thenReturn(true)
        whenever(urlFetcher.fetch("https://tumblbug.com/x")).thenReturn("<html/>")
        whenever(ogMetadataExtractor.extract("<html/>")).thenReturn(
            OgMetadata(title = "제목", imageUrl = null, description = null, url = null),
        )

        val response = service.prefill(PrefillRequest("https://tumblbug.com/x"), "203.0.113.10")

        assertEquals("https://tumblbug.com/x", response.originalUrl)
    }

    @Test
    fun `rejects when the rate limit is exceeded`() {
        whenever(rateLimiter.tryConsume("203.0.113.10")).thenReturn(false)

        assertThrows(BadRequestException::class.java) {
            service.prefill(PrefillRequest("https://tumblbug.com/x"), "203.0.113.10")
        }
    }

    @Test
    fun `rejects when no OG metadata is found at all`() {
        whenever(rateLimiter.tryConsume("203.0.113.10")).thenReturn(true)
        whenever(urlFetcher.fetch("https://tumblbug.com/x")).thenReturn("<html/>")
        whenever(ogMetadataExtractor.extract("<html/>")).thenReturn(
            OgMetadata(title = null, imageUrl = null, description = null, url = null),
        )

        assertThrows(BadRequestException::class.java) {
            service.prefill(PrefillRequest("https://tumblbug.com/x"), "203.0.113.10")
        }
    }
}
