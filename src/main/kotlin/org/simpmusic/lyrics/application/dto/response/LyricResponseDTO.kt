package org.simpmusic.lyrics.application.dto.response

/**
 * Response Data Transfer Object for Lyrics
 * Used for GET operations where server always returns an ID
 */
data class LyricResponseDTO(
    val id: String,
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
) 