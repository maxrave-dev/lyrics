package org.simpmusic.lyrics.domain.model

import org.simpmusic.lyrics.extensions.sha256

/**
 * Core domain entity representing song lyrics
 */
data class Lyric(
    val id: String,
    val videoId: String,
    val songTitle: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
    val plainLyric: String,
    val syncedLyrics: String?, // LRC format
    val richSyncLyrics: String?, // LRC format like Karaoke
    val vote: Int,
    val contributor: String,
    val contributorEmail: String,
    val sha256hash: String = "$videoId-$durationSeconds-$plainLyric-$syncedLyrics-$richSyncLyrics".sha256()
)
