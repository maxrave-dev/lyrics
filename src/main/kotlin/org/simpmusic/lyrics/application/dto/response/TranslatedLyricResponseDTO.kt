package org.simpmusic.lyrics.application.dto.response

import org.simpmusic.lyrics.application.dto.response.BaseResponseDTO

/**
 * Response Data Transfer Object for Translated Lyrics
 * Used for GET operations where server always returns an ID
 */
data class TranslatedLyricResponseDTO(
    val id: String,
    val videoId: String,
    val translatedLyric: String, // LRC format
    val language: String, // 2-letter code
    val vote: Int,
    val contributor: String,
    val contributorEmail: String,
) : BaseResponseDTO
