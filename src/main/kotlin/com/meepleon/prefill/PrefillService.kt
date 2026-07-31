package com.meepleon.prefill

import com.meepleon.common.BadRequestException
import org.springframework.stereotype.Service

@Service
class PrefillService(
    private val rateLimiter: PrefillRateLimiter,
    private val urlFetcher: UrlFetcher,
    private val ogMetadataExtractor: OgMetadataExtractor,
) {
    fun prefill(request: PrefillRequest, clientIp: String): PrefillResponse {
        if (!rateLimiter.tryConsume(clientIp)) {
            throw BadRequestException("Too many prefill requests, try again later")
        }
        val html = urlFetcher.fetch(request.url)
        val metadata = ogMetadataExtractor.extract(html)
        if (metadata.title == null && metadata.imageUrl == null && metadata.description == null) {
            throw BadRequestException("No Open Graph metadata found at the given URL")
        }
        return PrefillResponse(
            title = metadata.title,
            coverImageUrl = metadata.imageUrl,
            description = metadata.description,
            originalUrl = metadata.url ?: request.url,
        )
    }
}
