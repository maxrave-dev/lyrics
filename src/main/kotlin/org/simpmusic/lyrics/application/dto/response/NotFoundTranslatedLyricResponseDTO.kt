package org.simpmusic.lyrics.application.dto.response

import java.time.LocalDateTime

/**
 * Response Data Transfer Object for Not Found Translated Lyrics
 * Used for GET operations where server always returns an ID
 */
data class NotFoundTranslatedLyricResponseDTO(
    val id: String,
    val videoId: String,
    val translationLanguage: String,
    val addedDate: LocalDateTime
) : BaseResponseDTO 