package org.simpmusic.lyrics.domain.model

import java.time.LocalDateTime
import org.simpmusic.lyrics.extensions.sha256

/**
 * Domain model representing a not found translated lyric record
 */
data class NotFoundTranslatedLyric(
    val videoId: String,
    val translationLanguage: String,
    val addedDate: LocalDateTime,
    val sha256hash: String = "$videoId-$translationLanguage".sha256()
) 