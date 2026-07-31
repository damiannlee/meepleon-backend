package com.meepleon.prefill

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

data class OgMetadata(
    val title: String?,
    val imageUrl: String?,
    val description: String?,
    val url: String?,
)

/** Parses the standard Open Graph tags (ADR-0009) out of already-fetched HTML — no network calls of its own. */
@Component
class OgMetadataExtractor {
    fun extract(html: String): OgMetadata {
        val document = Jsoup.parse(html)
        return OgMetadata(
            title = ogContent(document, "og:title"),
            imageUrl = ogContent(document, "og:image"),
            description = ogContent(document, "og:description"),
            url = ogContent(document, "og:url"),
        )
    }

    private fun ogContent(document: Document, property: String): String? =
        document.select("meta[property=\"$property\"]")
            .firstOrNull()
            ?.attr("content")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}
