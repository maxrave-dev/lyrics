package org.simpmusic.lyrics.application.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import org.simpmusic.lyrics.extensions.sha256

/**
 * Request Data Transfer Object for Lyrics
 * Used for POST/PUT operations where client doesn't provide an ID
 */
data class LyricRequestDTO(
    val videoId: String,
    val songTitle: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
    val plainLyric: String,
    val syncedLyrics: String? = null,
    val richSyncLyrics: String? = null,
    val vote: Int = 0,
    val contributor: String,
    val contributorEmail: String,
) : BaseRequestDTO {
    @JsonIgnore
    override fun getUniqueId(): String = "$videoId-$durationSeconds-$plainLyric-$syncedLyrics-$richSyncLyrics".sha256()
}
