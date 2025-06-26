package org.simpmusic.lyrics.domain.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Domain model representing a translated lyric
 */
@OptIn(ExperimentalUuidApi::class)
data class TranslatedLyric(
    val id: Uuid,
    val videoId: String,
    val translatedLyric: String, // LRC format
    val language: String, // 2 letter code
    val vote: Int,
    val contributor: String,
    val contributorEmail: String
) 