package org.simpmusic.lyrics.extensions

import io.appwrite.ID
import io.appwrite.models.Document
import org.simpmusic.lyrics.application.dto.request.LyricRequestDTO
import org.simpmusic.lyrics.application.dto.request.NotFoundLyricRequestDTO
import org.simpmusic.lyrics.application.dto.request.TranslatedLyricRequestDTO
import org.simpmusic.lyrics.application.dto.response.LyricResponseDTO
import org.simpmusic.lyrics.application.dto.response.NotFoundLyricResponseDTO
import org.simpmusic.lyrics.application.dto.response.TranslatedLyricResponseDTO
import org.simpmusic.lyrics.domain.model.Lyric
import org.simpmusic.lyrics.domain.model.NotFoundLyric
import org.simpmusic.lyrics.domain.model.TranslatedLyric

fun Lyric.toResponseDTO(): LyricResponseDTO {
    return LyricResponseDTO(
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
        contributorEmail = contributorEmail
    )
}

fun LyricRequestDTO.toEntity(): Lyric {
    return Lyric(
        id = ID.unique(),
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
        contributorEmail = contributorEmail
    )
}

fun TranslatedLyric.toResponseDTO(): TranslatedLyricResponseDTO {
    return TranslatedLyricResponseDTO(
        id = id,
        videoId = videoId,
        translatedLyric = translatedLyric,
        language = language,
        vote = vote,
        contributor = contributor,
        contributorEmail = contributorEmail
    )
}

fun TranslatedLyricRequestDTO.toEntity(): TranslatedLyric {
    return TranslatedLyric(
        id = this.getUniqueId(),
        videoId = videoId,
        translatedLyric = translatedLyric,
        language = language,
        vote = vote,
        contributor = contributor,
        contributorEmail = contributorEmail
    )
}

fun NotFoundLyric.toResponseDTO(): NotFoundLyricResponseDTO {
    return NotFoundLyricResponseDTO(
        id = videoId,
        videoId = videoId,
        addedDate = addedDate
    )
}

fun NotFoundLyricRequestDTO.toEntity(): NotFoundLyric {
    return NotFoundLyric(
        videoId = videoId,
        addedDate = addedDate
    )
}

fun documentToTranslatedLyric(document: Document<Map<String, Any>>): TranslatedLyric {
    val videoId = document.data["videoId"] as String
    val translatedLyric = document.data["translatedLyric"] as String
    val language = document.data["language"] as String

    return TranslatedLyric(
        id = document.data["id"] as String,
        videoId = videoId,
        translatedLyric = translatedLyric,
        language = language,
        vote = (document.data["vote"] as Number).toInt(),
        contributor = document.data["contributor"] as String,
        contributorEmail = document.data["contributorEmail"] as String,
        sha256hash = document.data["sha256hash"]?.toString()
            ?: "$videoId-$language-$translatedLyric".sha256()
    )
}