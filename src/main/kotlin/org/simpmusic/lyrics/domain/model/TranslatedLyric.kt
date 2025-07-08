package org.simpmusic.lyrics.domain.model

import org.simpmusic.lyrics.extensions.sha256

/**
 * Domain model representing a translated lyric
 */
data class TranslatedLyric(
    val id: String,
    val videoId: String,
    val translatedLyric: String, // LRC format
    val language: String, // 2-letter code
    val vote: Int,
    val contributor: String,
    val contributorEmail: String,
    val sha256hash: String = "$videoId-$language-$translatedLyric".sha256()
) 