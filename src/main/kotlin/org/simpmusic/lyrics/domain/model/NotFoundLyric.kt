package org.simpmusic.lyrics.domain.model

import java.time.LocalDateTime

/**
 * Domain model representing a not found lyric record
 */
data class NotFoundLyric(
    val videoId: String,
    val addedDate: LocalDateTime
) 