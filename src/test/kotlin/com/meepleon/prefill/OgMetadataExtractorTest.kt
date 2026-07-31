package com.meepleon.prefill

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OgMetadataExtractorTest {

    private val extractor = OgMetadataExtractor()

    @Test
    fun `extracts all four og tags`() {
        val html = """
            <html><head>
              <meta property="og:title" content="논리 덕후 모여라!" />
              <meta property="og:image" content="https://img.example.com/cover.jpg" />
              <meta property="og:description" content="2인 추리전략 보드게임" />
              <meta property="og:url" content="https://tumblbug.com/boardgame-unseen" />
            </head></html>
        """.trimIndent()

        val metadata = extractor.extract(html)

        assertEquals("논리 덕후 모여라!", metadata.title)
        assertEquals("https://img.example.com/cover.jpg", metadata.imageUrl)
        assertEquals("2인 추리전략 보드게임", metadata.description)
        assertEquals("https://tumblbug.com/boardgame-unseen", metadata.url)
    }

    @Test
    fun `returns nulls when og tags are missing`() {
        val metadata = extractor.extract("<html><head><title>no og here</title></head></html>")

        assertNull(metadata.title)
        assertNull(metadata.imageUrl)
        assertNull(metadata.description)
        assertNull(metadata.url)
    }

    @Test
    fun `tolerates malformed html`() {
        val metadata = extractor.extract("<meta property=\"og:title\" content=\"broken\"><html unclosed")

        assertEquals("broken", metadata.title)
    }

    @Test
    fun `ignores a blank content attribute`() {
        val metadata = extractor.extract("<meta property=\"og:title\" content=\"   \" />")

        assertNull(metadata.title)
    }
}
