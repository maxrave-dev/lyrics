package org.simpmusic.lyrics.domain.model

import org.simpmusic.lyrics.extensions.sha256
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

/**
 * Domain model representing a translated lyric
 */
@Document(collection = "translated_lyrics")
data class TranslatedLyric(
    @Id
    @Field("_id")
    val id: String,
    val videoId: String,
    val translatedLyric: String, // LRC format
    val language: String, // 2-letter code
    val vote: Int,
    val contributor: String,
    val contributorEmail: String,
    val sha256hash: String = "$videoId-$language-$translatedLyric".sha256(),
)
