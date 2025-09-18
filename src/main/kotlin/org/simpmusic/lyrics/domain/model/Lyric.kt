package org.simpmusic.lyrics.domain.model

import org.simpmusic.lyrics.extensions.sha256
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

/**
 * Core domain entity representing song lyrics
 */
@Document(collection = "lyrics")
data class Lyric(
    @Id
    @Field("_id")
    val id: String,
    val videoId: String,
    val songTitle: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
    val plainLyric: String,
    val syncedLyrics: String?, // LRC format
    val richSyncLyrics: String, // LRC format like Karaoke
    val vote: Int,
    val contributor: String,
    val contributorEmail: String,
    val sha256hash: String = "$videoId-$durationSeconds-$plainLyric-$syncedLyrics-$richSyncLyrics".sha256(),
)
