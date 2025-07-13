package org.simpmusic.lyrics.application.dto.response

data class HmacResponseDTO(
    val uri: String,
    val timestamp: String,
    val hmac: String
)