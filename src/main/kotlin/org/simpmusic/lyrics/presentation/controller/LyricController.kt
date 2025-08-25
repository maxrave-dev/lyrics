@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.simpmusic.lyrics.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
import org.simpmusic.lyrics.application.dto.request.LyricRequestDTO
import org.simpmusic.lyrics.application.dto.request.TranslatedLyricRequestDTO
import org.simpmusic.lyrics.application.dto.request.VoteRequestDTO
import org.simpmusic.lyrics.application.dto.response.*
import org.simpmusic.lyrics.application.service.LyricService
import org.simpmusic.lyrics.application.service.MeilisearchService
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.uitls.DocsErrorResponse
import org.simpmusic.lyrics.uitls.DocsLyricResponseSuccess
import org.simpmusic.lyrics.uitls.DocsLyricsListResponseSuccess
import org.simpmusic.lyrics.uitls.DocsTranslatedLyricResponseSuccess
import org.simpmusic.lyrics.uitls.DocsTranslatedLyricsListResponseSuccess
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1")
class LyricController(
    private val lyricService: LyricService,
    private val meilisearchService: MeilisearchService,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val logger = LoggerFactory.getLogger(LyricController::class.java)

    // ========== Lyric API Endpoints ==========
    @Operation(
        summary = "Get Lyrics by Video ID",
        description = "Fetches lyrics associated with a specific video ID. Optionally supports pagination with limit and offset.",
        tags = ["Lyrics"],
        operationId = "getLyricsByVideoId",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved lyrics",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema =
                            Schema(
                                implementation = LyricResponseDTO::class,
                            ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Lyrics not found for the given video ID",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema =
                            Schema(
                                implementation = DocsErrorResponse::class,
                            ),
                    ),
                ],
            ),
        ],
    )
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
                    logger.error(
                        "getLyricsByVideoId --> Failed to get lyrics by videoId: ${result.message}",
                        result.exception,
                    )
                    val errorResponse = ErrorResponseDTO.serverError("Failed to get lyrics by videoId: $videoId")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
            }
        }

    @Operation(
        summary = "Search Lyrics by Song Title",
        description = "Searches for lyrics by song title with optional pagination using limit and offset parameters.",
        tags = ["Lyrics"],
        operationId = "getLyricsBySongTitle",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved lyrics matching the title",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsLyricsListResponseSuccess::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "No lyrics found for the given title",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error occurred while searching lyrics",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
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
                    logger.error(
                        "getLyricsBySongTitle --> Failed to get lyrics by title: ${result.message}",
                        result.exception,
                    )
                    val errorResponse = ErrorResponseDTO.serverError("Failed to get lyrics by title: $title")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
            }
        }

    @Operation(
        summary = "Search Lyrics by Artist",
        description = "Searches for lyrics by artist name with optional pagination using limit and offset parameters.",
        tags = ["Lyrics"],
        operationId = "getLyricsByArtist",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved lyrics by the artist",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsLyricsListResponseSuccess::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "No lyrics found for the given artist",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error occurred while searching lyrics",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
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
                    logger.error(
                        "getLyricsByArtist --> Failed to get lyrics by artist: ${result.message}",
                        result.exception,
                    )
                    val errorResponse = ErrorResponseDTO.serverError("Failed to get lyrics by artist: $artist")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
            }
        }

    @Operation(
        summary = "Search Lyrics",
        description = "Performs a general search across lyrics content with optional pagination using limit and offset parameters.",
        tags = ["Lyrics"],
        operationId = "searchLyrics",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved lyrics matching the search query",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsLyricsListResponseSuccess::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "No lyrics found for the search query",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error occurred while searching lyrics",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/search")
    suspend fun searchLyrics(
        @RequestParam q: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?,
    ): ResponseEntity<ApiResult<List<LyricResponseDTO>>> =
        withContext(ioDispatcher) {
            logger.debug("searchLyrics --> Searching lyrics with Meilisearch: $q, limit: $limit, offset: $offset")

            try {
                val searchResult = meilisearchService.searchLyrics(q, limit, offset).last()

                when (searchResult) {
                    is Resource.Success -> {
                        logger.debug("searchLyrics --> Found ${searchResult.data.size} IDs from Meilisearch for search: $q")

                        if (searchResult.data.isEmpty()) {
                            val errorResponse = ErrorResponseDTO.notFound("No lyrics found for search query: $q")
                            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                ApiResult.Error(
                                    error = errorResponse,
                                ),
                            )
                        } else {
                            val lyrics = mutableListOf<LyricResponseDTO>()

                            val tasks =
                                searchResult.data.map { searchResult ->
                                    async {
                                        val lyricResult = lyricService.getLyricById(searchResult.id).last()
                                        when (lyricResult) {
                                            is Resource.Success -> {
                                                lyricResult.data?.let { lyrics.add(it) }
                                            }

                                            is Resource.Error -> {
                                                logger.warn(
                                                    "searchLyrics --> Failed to get lyric by id: ${searchResult.id} - ${lyricResult.message}",
                                                )
                                            }
                                        }
                                    }
                                }
                            tasks.awaitAll()

                            logger.debug("searchLyrics --> Successfully retrieved ${lyrics.size} full lyrics for search: $q")
                            ResponseEntity.ok(
                                ApiResult.Success<List<LyricResponseDTO>>(
                                    data = lyrics,
                                ),
                            )
                        }
                    }

                    is Resource.Error -> {
                        logger.error(
                            "searchLyrics --> Failed to search lyrics with Meilisearch: ${searchResult.message}",
                            searchResult.exception,
                        )
                        val errorResponse = ErrorResponseDTO.serverError("Failed to search lyrics with query: $q")
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResult.Error(
                                error = errorResponse,
                            ),
                        )
                    }

                    else -> {
                        logger.warn("searchLyrics --> Unexpected resource state for search: $q")
                        val errorResponse = ErrorResponseDTO.serverError("Unexpected error while searching")
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResult.Error(
                                error = errorResponse,
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("searchLyrics --> Exception during search: ${e.message}", e)
                val errorResponse = ErrorResponseDTO.serverError("Failed to search lyrics with query: $q")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResult.Error(
                        error = errorResponse,
                    ),
                )
            }
        }

    @Operation(
        summary = "Create New Lyric",
        description = "Creates a new lyric entry with the provided lyric data.",
        tags = ["Lyrics"],
        operationId = "createLyric",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "Successfully created lyric",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsLyricResponseSuccess::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid lyric data",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error occurred while creating lyric",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
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
            }
        }

    // ========== TranslatedLyrics API Endpoints ==========

    @Operation(
        summary = "Get Translated Lyrics by Video ID",
        description = "Fetches translated lyrics for a specific video ID with optional pagination using limit and offset parameters.",
        tags = ["Translated Lyrics"],
        operationId = "getTranslatedLyricsByVideoId",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved translated lyrics",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsTranslatedLyricsListResponseSuccess::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "No translated lyrics found for the given video ID",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error occurred while retrieving translated lyrics",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
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
                        val errorResponse =
                            ErrorResponseDTO.notFound("Translated lyrics not found for videoId: $videoId")
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
                    val errorResponse =
                        ErrorResponseDTO.serverError("Failed to get translated lyrics for videoId: $videoId")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
            }
        }

    @Operation(
        summary = "Get Translated Lyric by Video ID and Language",
        description = "Fetches a specific translated lyric for a video ID in a particular language.",
        tags = ["Translated Lyrics"],
        operationId = "getTranslatedLyricByVideoIdAndLanguage",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved translated lyric",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsTranslatedLyricResponseSuccess::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "No translated lyric found for the given video ID and language",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error occurred while retrieving translated lyric",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
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
            }
        }

    @Operation(
        summary = "Create New Translated Lyric",
        description = "Creates a new translated lyric entry with the provided translated lyric data.",
        tags = ["Translated Lyrics"],
        operationId = "createTranslatedLyric",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "Successfully created translated lyric",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsTranslatedLyricResponseSuccess::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid translated lyric data",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error occurred while creating translated lyric",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
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
                    logger.error(
                        "createTranslatedLyric --> Failed to create translated lyric: ${result.message}",
                        result.exception,
                    )
                    val errorResponse = result.toErrorResponse()
                    ResponseEntity.status(HttpStatus.valueOf(errorResponse.code)).body(
                        ApiResult.Error(
                            error = errorResponse,
                        ),
                    )
                }
            }
        }

    // ========== Vote API Endpoints ==========

    @Operation(
        summary = "Vote for a Lyric",
        description = "Allows users to vote for a lyric by its ID. Returns the updated lyric data.",
        tags = ["Lyrics"],
        operationId = "voteLyric",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully voted for lyric",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema =
                            Schema(
                                implementation = DocsLyricResponseSuccess::class,
                            ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Error occurred while processing the vote",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema =
                            Schema(
                                implementation = DocsErrorResponse::class,
                            ),
                    ),
                ],
            ),
        ],
    )
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
            }
        }

    @Operation(
        summary = "Vote for a Translated Lyric",
        description = "Allows users to vote for a translated lyric by its ID. Returns the updated translated lyric data.",
        tags = ["Translated Lyrics"],
        operationId = "voteTranslatedLyric",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully voted for translated lyric",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsTranslatedLyricResponseSuccess::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Error occurred while processing the vote for translated lyric",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error occurred while processing the vote",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DocsErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
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
            }
        }
}
