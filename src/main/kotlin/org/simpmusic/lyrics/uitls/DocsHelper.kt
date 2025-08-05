package org.simpmusic.lyrics.uitls

import org.simpmusic.lyrics.application.dto.response.ErrorResponseDTO
import org.simpmusic.lyrics.application.dto.response.LyricResponseDTO
import org.simpmusic.lyrics.application.dto.response.TranslatedLyricResponseDTO

data class DocsErrorResponse(
    val error: ErrorResponseDTO,
    val success: Boolean = false,
)

data class DocsLyricResponseSuccess(
    val data: LyricResponseDTO,
    val success: Boolean = true,
)

data class DocsLyricsListResponseSuccess(
    val data: List<LyricResponseDTO>,
    val success: Boolean = true,
)

data class DocsTranslatedLyricResponseSuccess(
    val data: TranslatedLyricResponseDTO,
    val success: Boolean = true,
)

data class DocsTranslatedLyricsListResponseSuccess(
    val data: List<TranslatedLyricResponseDTO>,
    val success: Boolean = true,
)
