package org.simpmusic.lyrics.application.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import org.simpmusic.lyrics.extensions.sha256
import java.time.LocalDateTime

/**
 * Request Data Transfer Object for Not Found Lyrics
 * Used for POST/PUT operations where client doesn't provide an ID
 */
data class NotFoundLyricRequestDTO(
    val videoId: String,
    val addedDate: LocalDateTime = LocalDateTime.now(),
) : BaseRequestDTO {
    @JsonIgnore
    override fun getUniqueId(): String = videoId.sha256()
} 
