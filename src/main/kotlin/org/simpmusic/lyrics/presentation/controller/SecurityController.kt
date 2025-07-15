package org.simpmusic.lyrics.presentation.controller

import org.simpmusic.lyrics.application.dto.response.HmacResponseDTO
import org.simpmusic.lyrics.infrastructure.config.HmacService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.Instant

/**
 * Controller for security-related operations
 * ONLY for development purposes
 */
//@RestController
//@RequestMapping("/api/security")
class SecurityController(private val hmacService: HmacService) {

    /**
     * @param uri URI to generate token for (e.g. /api/lyrics)
     * @return Map with timestamp and HMAC token
     */
    @GetMapping("/generate-hmac")
    fun generateHmacExample(@RequestParam uri: String): HmacResponseDTO {
        val timestamp = Instant.now().toEpochMilli().toString()
        val data = "$timestamp$uri"
        val hmac = hmacService.generateHmac(data)

        return HmacResponseDTO(
            uri = uri,
            timestamp = timestamp,
            hmac = hmac
        )
    }
}