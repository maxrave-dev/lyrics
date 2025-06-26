package org.simpmusic.lyrics.application.dto

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Data Transfer Object for Translated Lyrics
 * Used to communicate between presentation layer and application layer
 */
@OptIn(ExperimentalUuidApi::class)
data class TranslatedLyricDTO(
    val id: String = Uuid.random().toString(),
    val videoId: String,
    val translatedLyric: String, // LRC format
    val language: String, // 2 letter code
    val vote: Int,
    val contributor: String,
    val contributorEmail: String
) 