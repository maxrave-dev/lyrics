package org.simpmusic.lyrics.application.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import org.simpmusic.lyrics.application.dto.BaseDTO
import org.simpmusic.lyrics.extensions.sha256

/**
 * Request Data Transfer Object for Translated Lyrics
 * Used for POST/PUT operations where client doesn't provide an ID
 */
data class TranslatedLyricRequestDTO(
    val videoId: String,
    val translatedLyric: String, // LRC format
    val language: String, // 2-letter code
    val vote: Int = 0,
    val contributor: String,
    val contributorEmail: String
): BaseDTO {
    @JsonIgnore
    override fun getUniqueId(): String {
        return "$videoId-$language-$translatedLyric".sha256()
    }
} 