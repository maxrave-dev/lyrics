package org.simpmusic.lyrics.application.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import org.simpmusic.lyrics.extensions.sha256

/**
 * Data Transfer Object for Lyrics
 * Used to communicate between presentation layer and application layer
 */
data class LyricDTO(
    val videoId: String,
    val songTitle: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
    val plainLyric: String,
    val syncedLyrics: String?,
    val richSyncLyrics: String?,
    val vote: Int,
    val contributor: String,
    val contributorEmail: String
): BaseDTO {
    @JsonIgnore
    override fun getUniqueId(): String {
        return "$videoId-$durationSeconds-$plainLyric-$syncedLyrics-$richSyncLyrics".sha256()
    }
}
