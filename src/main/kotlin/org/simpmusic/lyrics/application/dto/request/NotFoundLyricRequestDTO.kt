package org.simpmusic.lyrics.application.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import org.simpmusic.lyrics.application.dto.BaseDTO
import org.simpmusic.lyrics.extensions.sha256
import java.time.LocalDateTime

/**
 * Request Data Transfer Object for Not Found Lyrics
 * Used for POST/PUT operations where client doesn't provide an ID
 */
data class NotFoundLyricRequestDTO(
    val videoId: String,
    val addedDate: LocalDateTime = LocalDateTime.now()
): BaseDTO {
    @JsonIgnore
    override fun getUniqueId(): String {
        return videoId.sha256()
    }
} 