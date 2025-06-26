package org.simpmusic.lyrics.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Domain model representing a not found lyric record
 */
@OptIn(ExperimentalUuidApi::class)
data class NotFoundLyric(
    val id: Uuid,
    val videoId: String,
    val addedDate: LocalDateTime
) 