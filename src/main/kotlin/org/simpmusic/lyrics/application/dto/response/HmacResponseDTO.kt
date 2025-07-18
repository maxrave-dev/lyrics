package org.simpmusic.lyrics.application.dto.response

import org.simpmusic.lyrics.application.dto.response.BaseResponseDTO

data class HmacResponseDTO(
    val uri: String,
    val timestamp: String,
    val hmac: String,
) : BaseResponseDTO
