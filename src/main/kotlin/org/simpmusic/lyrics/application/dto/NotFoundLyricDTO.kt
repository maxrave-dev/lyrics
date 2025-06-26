package org.simpmusic.lyrics.application.dto

import java.time.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Data Transfer Object for Not Found Lyrics
 * Used to communicate between presentation layer and application layer
 */
@OptIn(ExperimentalUuidApi::class)
data class NotFoundLyricDTO(
    val id: String = Uuid.random().toString(),
    val videoId: String,
    val addedDate: LocalDateTime = LocalDateTime.now()
) 