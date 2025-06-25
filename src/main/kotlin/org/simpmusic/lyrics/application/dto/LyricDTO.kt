package org.simpmusic.lyrics.application.dto

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Data Transfer Object for Lyrics
 * Used to communicate between presentation layer and application layer
 */
data class LyricDTO @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val videoId: String,
    val songTitle: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
    val plainLyric: String,
    val syncedLyrics: String,
    val richSyncLyrics: String,
    val vote: Int,
    val contributor: String,
    val contributorEmail: String
)
