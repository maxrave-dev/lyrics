@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.simpmusic.lyrics.presentation.controller

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
import org.simpmusic.lyrics.application.dto.request.*
import org.simpmusic.lyrics.application.dto.response.*
import org.simpmusic.lyrics.application.service.LyricService
import org.simpmusic.lyrics.domain.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1")
class LyricController(
    private val lyricService: LyricService,
    @Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
) {
    private val logger = LoggerFactory.getLogger(LyricController::class.java)

    @GetMapping("/{videoId}")
    suspend fun getLyricsByVideoId(
        @PathVariable videoId: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?,
    ): ResponseEntity<ApiResult<List<LyricResponseDTO>>> =
        withContext(ioDispatcher) {
            logger.debug("getLyricsByVideoId --> Getting lyrics for videoId: $videoId, limit: $limit, offset: $offset")
            val result = lyricService.getLyricsByVideoId(videoId, limit, offset).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("getLyricsByVideoId --> Found ${result.data.size} lyrics for videoId: $videoId")
                    if (result.data.isNotEmpty()) {
                        ResponseEntity.ok(
                            ApiResult.Success<List<LyricResponseDTO>>(
                                data = result.data,
                            ),
                        )
                    } else {
                        val errorResponse = ErrorResponseDTO.notFound("Lyrics not found for videoId: $videoId")
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            ApiResult.Error(
                                error = errorResponse,
                            ),
                        )
                    }
                }
                is Resource.Error -> {
                    logger.error("getLyricsByVideoId --> Failed to get lyrics by videoId: ${result.message}", result.exception)
                    val errorResponse = ErrorResponseDTO.serverError("Failed to get lyrics by videoId: $videoId")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
                is Resource.Loading ->
                    ResponseEntity.status(HttpStatus.PROCESSING).body(
                        ApiResult.Loading<List<LyricResponseDTO>>(
                            processing =
                                LoadingResponseDTO.fromMessage(
                                    HttpStatus.PROCESSING.value(),
                                    "Processing request to get lyrics for videoId: $videoId",
                                ),
                        ),
                    )
            }
        }

    @GetMapping("/search/title")
    suspend fun getLyricsBySongTitle(
        @RequestParam title: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?,
    ): ResponseEntity<ApiResult<List<LyricResponseDTO>>> =
        withContext(ioDispatcher) {
            logger.debug("getLyricsBySongTitle --> Searching lyrics by title: $title, limit: $limit, offset: $offset")
            val result = lyricService.getLyricsBySongTitle(title, limit, offset).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("getLyricsBySongTitle --> Found ${result.data.size} lyrics for title: $title")
                    if (result.data.isNotEmpty()) {
                        ResponseEntity.ok(
                            ApiResult.Success<List<LyricResponseDTO>>(
                                data = result.data,
                            ),
                        )
                    } else {
                        val errorResponse = ErrorResponseDTO.notFound("Lyrics not found for title: $title")
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            ApiResult.Error(
                                error = errorResponse,
                            ),
                        )
                    }
                }
                is Resource.Error -> {
                    logger.error("getLyricsBySongTitle --> Failed to get lyrics by title: ${result.message}", result.exception)
                    val errorResponse = ErrorResponseDTO.serverError("Failed to get lyrics by title: $title")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
                is Resource.Loading ->
                    ResponseEntity.status(HttpStatus.PROCESSING).body(
                        ApiResult.Loading<List<LyricResponseDTO>>(
                            processing =
                                LoadingResponseDTO.fromMessage(
                                    HttpStatus.PROCESSING.value(),
                                    "Processing request to get lyrics for title: $title",
                                ),
                        ),
                    )
            }
        }

    @GetMapping("/search/artist")
    suspend fun getLyricsByArtist(
        @RequestParam artist: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?,
    ): ResponseEntity<ApiResult<List<LyricResponseDTO>>> =
        withContext(ioDispatcher) {
            logger.debug("getLyricsByArtist --> Searching lyrics by artist: $artist, limit: $limit, offset: $offset")
            val result = lyricService.getLyricsByArtist(artist, limit, offset).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("getLyricsByArtist --> Found ${result.data.size} lyrics for artist: $artist")
                    if (result.data.isNotEmpty()) {
                        ResponseEntity.ok(
                            ApiResult.Success<List<LyricResponseDTO>>(
                                data = result.data,
                            ),
                        )
                    } else {
                        val errorResponse = ErrorResponseDTO.notFound("Lyrics not found for artist: $artist")
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            ApiResult.Error(
                                error = errorResponse,
                            ),
                        )
                    }
                }
                is Resource.Error -> {
                    logger.error("getLyricsByArtist --> Failed to get lyrics by artist: ${result.message}", result.exception)
                    val errorResponse = ErrorResponseDTO.serverError("Failed to get lyrics by artist: $artist")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
                is Resource.Loading ->
                    ResponseEntity.status(HttpStatus.PROCESSING).body(
                        ApiResult.Loading<List<LyricResponseDTO>>(
                            processing =
                                LoadingResponseDTO.fromMessage(
                                    HttpStatus.PROCESSING.value(),
                                    "Processing request to get lyrics for artist: $artist",
                                ),
                        ),
                    )
            }
        }

    @GetMapping("/search")
    suspend fun searchLyrics(
        @RequestParam q: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?,
    ): ResponseEntity<ApiResult<List<LyricResponseDTO>>> =
        withContext(ioDispatcher) {
            logger.debug("searchLyrics --> Searching lyrics with keywords: $q, limit: $limit, offset: $offset")
            val result = lyricService.searchLyrics(q, limit, offset).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("searchLyrics --> Found ${result.data.size} lyrics for search: $q")
                    if (result.data.isNotEmpty()) {
                        ResponseEntity.ok(
                            ApiResult.Success<List<LyricResponseDTO>>(
                                data = result.data,
                            ),
                        )
                    } else {
                        val errorResponse = ErrorResponseDTO.notFound("No lyrics found for search query: $q")
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            ApiResult.Error(
                                error = errorResponse,
                            ),
                        )
                    }
                }
                is Resource.Error -> {
                    logger.error("searchLyrics --> Failed to search lyrics: ${result.message}", result.exception)
                    val errorResponse = ErrorResponseDTO.serverError("Failed to search lyrics with query: $q")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
                is Resource.Loading ->
                    ResponseEntity.status(HttpStatus.PROCESSING).body(
                        ApiResult.Loading<List<LyricResponseDTO>>(
                            processing =
                                LoadingResponseDTO.fromMessage(
                                    HttpStatus.PROCESSING.value(),
                                    "Processing search request with query: $q",
                                ),
                        ),
                    )
            }
        }

    @PostMapping
    suspend fun createLyric(
        @RequestBody lyricRequestDTO: LyricRequestDTO,
    ): ResponseEntity<ApiResult<LyricResponseDTO>> =
        withContext(ioDispatcher) {
            logger.debug("createLyric --> Creating Lyric")
            val result = lyricService.saveLyric(lyricRequestDTO).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("createLyric --> Successfully created lyric")
                    ResponseEntity.status(HttpStatus.CREATED).body(
                        ApiResult.Success<LyricResponseDTO>(
                            data = result.data,
                        ),
                    )
                }
                is Resource.Error -> {
                    logger.error("createLyric --> Failed to create lyric: ${result.message}", result.exception)
                    val errorResponse = result.toErrorResponse()
                    ResponseEntity.status(HttpStatus.valueOf(errorResponse.code)).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
                is Resource.Loading ->
                    ResponseEntity.status(HttpStatus.PROCESSING).body(
                        ApiResult.Loading<LyricResponseDTO>(
                            processing =
                                LoadingResponseDTO.fromMessage(
                                    HttpStatus.PROCESSING.value(),
                                    "Processing lyric creation request",
                                ),
                        ),
                    )
            }
        }

    // ========== TranslatedLyrics API Endpoints ==========

    @GetMapping("/translated/{videoId}")
    suspend fun getTranslatedLyricsByVideoId(
        @PathVariable videoId: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?,
    ): ResponseEntity<ApiResult<List<TranslatedLyricResponseDTO>>> =
        withContext(ioDispatcher) {
            logger.debug("getTranslatedLyricsByVideoId --> Getting translated lyrics for videoId: $videoId, limit: $limit, offset: $offset")
            val result = lyricService.getTranslatedLyricsByVideoId(videoId, limit, offset).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("getTranslatedLyricsByVideoId --> Found ${result.data.size} translated lyrics for videoId: $videoId")
                    if (result.data.isNotEmpty()) {
                        ResponseEntity.ok(
                            ApiResult.Success<List<TranslatedLyricResponseDTO>>(
                                data = result.data,
                            ),
                        )
                    } else {
                        val errorResponse = ErrorResponseDTO.notFound("Translated lyrics not found for videoId: $videoId")
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            ApiResult.Error(
                                error = errorResponse,
                            ),
                        )
                    }
                }
                is Resource.Error -> {
                    logger.error(
                        "getTranslatedLyricsByVideoId --> Failed to get translated lyrics by videoId: ${result.message}",
                        result.exception,
                    )
                    val errorResponse = ErrorResponseDTO.serverError("Failed to get translated lyrics for videoId: $videoId")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
                is Resource.Loading ->
                    ResponseEntity.status(HttpStatus.PROCESSING).body(
                        ApiResult.Loading<List<TranslatedLyricResponseDTO>>(
                            processing =
                                LoadingResponseDTO.fromMessage(
                                    HttpStatus.PROCESSING.value(),
                                    "Processing request to get translated lyrics for videoId: $videoId",
                                ),
                        ),
                    )
            }
        }

    @GetMapping("/translated/{videoId}/{language}")
    suspend fun getTranslatedLyricByVideoIdAndLanguage(
        @PathVariable videoId: String,
        @PathVariable language: String,
    ): ResponseEntity<ApiResult<TranslatedLyricResponseDTO>> =
        withContext(ioDispatcher) {
            logger.debug("Getting translated lyrics for videoId: $videoId, language: $language")
            val result = lyricService.getTranslatedLyricByVideoIdAndLanguage(videoId, language).last()
            when (result) {
                is Resource.Success -> {
                    result.data?.let {
                        logger.debug("Found translated lyrics for videoId: $videoId, language: $language")
                        ResponseEntity.ok(
                            ApiResult.Success<TranslatedLyricResponseDTO>(
                                data = it,
                            ),
                        )
                    } ?: run {
                        val errorResponse =
                            ErrorResponseDTO.notFound(
                                "Translated lyrics not found for videoId: $videoId and language: $language",
                            )
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            ApiResult.Error(
                                error = errorResponse,
                            ),
                        )
                    }
                }
                is Resource.Error -> {
                    logger.error("Failed to get translated lyrics: ${result.message}", result.exception)
                    val errorResponse =
                        ErrorResponseDTO.serverError(
                            "Failed to get translated lyrics for videoId: $videoId and language: $language",
                        )
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
                is Resource.Loading ->
                    ResponseEntity.status(HttpStatus.PROCESSING).body(
                        ApiResult.Loading<TranslatedLyricResponseDTO>(
                            processing =
                                LoadingResponseDTO.fromMessage(
                                    HttpStatus.PROCESSING.value(),
                                    "Processing request to get translated lyrics for videoId: $videoId and language: $language",
                                ),
                        ),
                    )
            }
        }

    @PostMapping("/translated")
    suspend fun createTranslatedLyric(
        @RequestBody translatedLyricRequestDTO: TranslatedLyricRequestDTO,
    ): ResponseEntity<ApiResult<TranslatedLyricResponseDTO>> =
        withContext(ioDispatcher) {
            logger.debug("createTranslatedLyric --> Creating translated lyric")
            val result = lyricService.saveTranslatedLyric(translatedLyricRequestDTO).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("createTranslatedLyric --> Successfully created translated lyric")
                    ResponseEntity.status(HttpStatus.CREATED).body(
                        ApiResult.Success<TranslatedLyricResponseDTO>(
                            data = result.data,
                        ),
                    )
                }
                is Resource.Error -> {
                    logger.error("createTranslatedLyric --> Failed to create translated lyric: ${result.message}", result.exception)
                    val errorResponse = result.toErrorResponse()
                    ResponseEntity.status(HttpStatus.valueOf(errorResponse.code)).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
                is Resource.Loading ->
                    ResponseEntity.status(HttpStatus.PROCESSING).body(
                        ApiResult.Loading<TranslatedLyricResponseDTO>(
                            processing =
                                LoadingResponseDTO.fromMessage(
                                    HttpStatus.PROCESSING.value(),
                                    "Processing translated lyric creation request",
                                ),
                        ),
                    )
            }
        }

    // ========== Vote API Endpoints ==========

    @PostMapping("/vote")
    suspend fun voteLyric(
        @RequestBody voteDTO: VoteRequestDTO,
    ): ResponseEntity<ApiResult<LyricResponseDTO>> =
        withContext(ioDispatcher) {
            logger.debug("voteLyric --> Processing vote request for lyric id: ${voteDTO.id}")
            val result = lyricService.voteLyric(voteDTO).last()

            when (result) {
                is Resource.Success -> {
                    logger.debug("voteLyric --> Successfully processed vote for lyric id: ${voteDTO.id}")
                    ResponseEntity.ok(
                        ApiResult.Success<LyricResponseDTO>(
                            data = result.data,
                        ),
                    )
                }
                is Resource.Error -> {
                    logger.error("voteLyric --> Failed to process vote: ${result.message}", result.exception)
                    val errorResponse = result.toErrorResponse()
                    ResponseEntity.status(HttpStatus.valueOf(errorResponse.code)).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
                is Resource.Loading ->
                    ResponseEntity.status(HttpStatus.PROCESSING).body(
                        ApiResult.Loading<LyricResponseDTO>(
                            processing =
                                LoadingResponseDTO.fromMessage(
                                    HttpStatus.PROCESSING.value(),
                                    "Processing vote for lyric id: ${voteDTO.id}",
                                ),
                        ),
                    )
            }
        }

    @PostMapping("/translated/vote")
    suspend fun voteTranslatedLyric(
        @RequestBody voteDTO: VoteRequestDTO,
    ): ResponseEntity<ApiResult<TranslatedLyricResponseDTO>> =
        withContext(ioDispatcher) {
            logger.debug("voteTranslatedLyric --> Processing vote request for translated lyric id: ${voteDTO.id}")
            val result = lyricService.voteTranslatedLyric(voteDTO).last()

            when (result) {
                is Resource.Success -> {
                    logger.debug("voteTranslatedLyric --> Successfully processed vote for translated lyric id: ${voteDTO.id}")
                    ResponseEntity.ok(
                        ApiResult.Success<TranslatedLyricResponseDTO>(
                            data = result.data,
                        ),
                    )
                }
                is Resource.Error -> {
                    logger.error("voteTranslatedLyric --> Failed to process vote: ${result.message}", result.exception)
                    val errorResponse = result.toErrorResponse()
                    ResponseEntity.status(HttpStatus.valueOf(errorResponse.code)).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
                is Resource.Loading ->
                    ResponseEntity.status(HttpStatus.PROCESSING).body(
                        ApiResult.Loading<TranslatedLyricResponseDTO>(
                            processing =
                                LoadingResponseDTO.fromMessage(
                                    HttpStatus.PROCESSING.value(),
                                    "Processing vote for translated lyric id: ${voteDTO.id}",
                                ),
                        ),
                    )
            }
        }
}
