package org.simpmusic.lyrics.application.dto

import org.simpmusic.lyrics.extensions.sha256
import java.time.LocalDateTime

/**
 * Data Transfer Object for Not Found Lyrics
 * Used to communicate between presentation layer and application layer
 */
data class NotFoundLyricDTO(
    val videoId: String,
    val addedDate: LocalDateTime = LocalDateTime.now()
): BaseDTO {
    override fun getUniqueId(): String {
        return videoId.sha256()
    }
}