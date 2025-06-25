package org.simpmusic.lyrics.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Core domain entity representing song lyrics
 */
data class Lyric @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val videoId: String,
    val songTitle: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
    val plainLyric: String,
    val syncedLyrics: String, // LRC format
    val richSyncLyrics: String, // LRC format like Karaoke
    val vote: Int,
    val contributor: String,
    val contributorEmail: String,
)
