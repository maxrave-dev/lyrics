package org.simpmusic.lyrics.application.dto.response

import org.simpmusic.lyrics.application.dto.response.BaseResponseDTO
import java.time.LocalDateTime

/**
 * Response Data Transfer Object for Not Found Lyrics
 * Used for GET operations where server always returns an ID
 */
data class NotFoundLyricResponseDTO(
    val id: String,
    val videoId: String,
    val addedDate: LocalDateTime,
) : BaseResponseDTO
