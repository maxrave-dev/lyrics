package org.simpmusic.lyrics.extensions

import org.simpmusic.lyrics.application.dto.request.LyricRequestDTO
import org.simpmusic.lyrics.application.dto.request.TranslatedLyricRequestDTO
import org.simpmusic.lyrics.application.dto.response.LyricResponseDTO
import org.simpmusic.lyrics.application.dto.response.TranslatedLyricResponseDTO
import org.simpmusic.lyrics.domain.model.Lyric
import org.simpmusic.lyrics.domain.model.TranslatedLyric
import org.simpmusic.lyrics.uitls.ID

fun Lyric.toResponseDTO(): LyricResponseDTO =
    LyricResponseDTO(
        id = id,
        videoId = videoId,
        songTitle = songTitle,
        artistName = artistName,
        albumName = albumName,
        durationSeconds = durationSeconds,
        plainLyric = plainLyric,
        syncedLyrics = syncedLyrics,
        richSyncLyrics = richSyncLyrics,
        vote = vote,
        contributor = contributor,
        contributorEmail = contributorEmail,
    )

fun LyricRequestDTO.toEntity(): Lyric =
    Lyric(
        id = ID.unique(),
        videoId = videoId,
        songTitle = songTitle,
        artistName = artistName,
        albumName = albumName,
        durationSeconds = durationSeconds,
        plainLyric = plainLyric,
        syncedLyrics = syncedLyrics,
        richSyncLyrics = richSyncLyrics ?: "",
        vote = vote,
        contributor = contributor,
        contributorEmail = contributorEmail,
    )

fun TranslatedLyric.toResponseDTO(): TranslatedLyricResponseDTO =
    TranslatedLyricResponseDTO(
        id = id,
        videoId = videoId,
        translatedLyric = translatedLyric,
        language = language,
        vote = vote,
        contributor = contributor,
        contributorEmail = contributorEmail,
    )

fun TranslatedLyricRequestDTO.toEntity(): TranslatedLyric =
    TranslatedLyric(
        id = ID.unique(),
        videoId = videoId,
        translatedLyric = translatedLyric,
        language = language,
        vote = vote,
        contributor = contributor,
        contributorEmail = contributorEmail,
    )
