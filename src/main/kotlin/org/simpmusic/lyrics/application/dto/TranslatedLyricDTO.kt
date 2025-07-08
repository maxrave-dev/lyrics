package org.simpmusic.lyrics.application.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import org.simpmusic.lyrics.extensions.sha256

/**
 * Data Transfer Object for Translated Lyrics
 * Used to communicate between presentation layer and application layer
 */
data class TranslatedLyricDTO(
    val videoId: String,
    val translatedLyric: String, // LRC format
    val language: String, // 2-letter code
    val vote: Int,
    val contributor: String,
    val contributorEmail: String
): BaseDTO {
    @JsonIgnore
    override fun getUniqueId(): String {
        return "$videoId-$language-$translatedLyric".sha256()
    }
}