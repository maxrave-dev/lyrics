package org.simpmusic.lyrics.presentation.controller

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.simpmusic.lyrics.application.dto.LyricDTO
import org.simpmusic.lyrics.application.dto.TranslatedLyricDTO
import org.simpmusic.lyrics.application.dto.NotFoundLyricDTO
import org.simpmusic.lyrics.application.dto.ErrorResponseDTO
import org.simpmusic.lyrics.application.dto.VoteDTO
import org.simpmusic.lyrics.application.dto.request.LyricRequestDTO
import org.simpmusic.lyrics.application.dto.request.NotFoundLyricRequestDTO
import org.simpmusic.lyrics.application.dto.request.TranslatedLyricRequestDTO
import org.simpmusic.lyrics.application.dto.response.LyricResponseDTO
import org.simpmusic.lyrics.application.dto.response.NotFoundLyricResponseDTO
import org.simpmusic.lyrics.application.dto.response.TranslatedLyricResponseDTO
import org.simpmusic.lyrics.application.service.LyricService
import org.simpmusic.lyrics.domain.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/lyrics")
class LyricController(
    private val lyricService: LyricService,
    @Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) {
    private val logger = LoggerFactory.getLogger(LyricController::class.java)

    @GetMapping("/{videoId}")
    suspend fun getLyricsByVideoId(
        @PathVariable videoId: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?
    ): ResponseEntity<List<LyricResponseDTO>> {
        return withContext(ioDispatcher) {
            logger.debug("getLyricsByVideoId --> Getting lyrics for videoId: $videoId, limit: $limit, offset: $offset")
            val result = lyricService.getLyricsByVideoId(videoId, limit, offset).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("getLyricsByVideoId --> Found ${result.data.size} lyrics for videoId: $videoId")
                    if (result.data.isNotEmpty()) {
                        ResponseEntity.ok(result.data)
                    } else {
                        ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                    }
                }
                is Resource.Error -> {
                    logger.error("getLyricsByVideoId --> Failed to get lyrics by videoId: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @GetMapping("/search/title")
    suspend fun getLyricsBySongTitle(
        @RequestParam title: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?
    ): ResponseEntity<List<LyricResponseDTO>> {
        return withContext(ioDispatcher) {
            logger.debug("getLyricsBySongTitle --> Searching lyrics by title: $title, limit: $limit, offset: $offset")
            val result = lyricService.getLyricsBySongTitle(title, limit, offset).last()
            when (result) {
                is Resource.Success -> ResponseEntity.ok(result.data)
                is Resource.Error -> {
                    logger.error("getLyricsBySongTitle --> Failed to get lyrics by title: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @GetMapping("/search/artist")
    suspend fun getLyricsByArtist(
        @RequestParam artist: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?
    ): ResponseEntity<List<LyricResponseDTO>> {
        return withContext(ioDispatcher) {
            logger.debug("getLyricsByArtist --> Searching lyrics by artist: $artist, limit: $limit, offset: $offset")
            val result = lyricService.getLyricsByArtist(artist, limit, offset).last()
            when (result) {
                is Resource.Success -> ResponseEntity.ok(result.data)
                is Resource.Error -> {
                    logger.error("getLyricsByArtist --> Failed to get lyrics by artist: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @GetMapping("/search")
    suspend fun searchLyrics(
        @RequestParam q: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?
    ): ResponseEntity<List<LyricResponseDTO>> {
        return withContext(ioDispatcher) {
            logger.debug("searchLyrics --> Searching lyrics with keywords: $q, limit: $limit, offset: $offset")
            val result = lyricService.searchLyrics(q, limit, offset).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("searchLyrics --> Found ${result.data.size} lyrics for search: $q")
                    ResponseEntity.ok(result.data)
                }
                is Resource.Error -> {
                    logger.error("searchLyrics --> Failed to search lyrics: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @PostMapping
    suspend fun createLyric(@RequestBody lyricRequestDTO: LyricRequestDTO): ResponseEntity<Any> {
        return withContext(ioDispatcher) {
            logger.debug("createLyric --> Creating Lyric")
            val result = lyricService.saveLyric(lyricRequestDTO).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("createLyric --> Successfully created lyric")
                    ResponseEntity.status(HttpStatus.CREATED).body(result.data)
                }
                is Resource.Error -> {
                    logger.error("createLyric --> Failed to create lyric: ${result.message}", result.exception)
                    val errorResponse = result.toErrorResponse()
                    ResponseEntity.status(HttpStatus.valueOf(errorResponse.code)).body(errorResponse)
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
    
    // ========== TranslatedLyrics API Endpoints ==========
    
    @GetMapping("/translated/{videoId}")
    suspend fun getTranslatedLyricsByVideoId(
        @PathVariable videoId: String,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?
    ): ResponseEntity<List<TranslatedLyricResponseDTO>> {
        return withContext(ioDispatcher) {
            logger.debug("getTranslatedLyricsByVideoId --> Getting translated lyrics for videoId: $videoId, limit: $limit, offset: $offset")
            val result = lyricService.getTranslatedLyricsByVideoId(videoId, limit, offset).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("getTranslatedLyricsByVideoId --> Found ${result.data.size} translated lyrics for videoId: $videoId")
                    ResponseEntity.ok(result.data)
                }
                is Resource.Error -> {
                    logger.error("getTranslatedLyricsByVideoId --> Failed to get translated lyrics by videoId: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
    
    @GetMapping("/translated/{videoId}/{language}")
    suspend fun getTranslatedLyricByVideoIdAndLanguage(
        @PathVariable videoId: String,
        @PathVariable language: String
    ): ResponseEntity<TranslatedLyricResponseDTO> {
        return withContext(ioDispatcher) {
            logger.debug("Getting translated lyrics for videoId: $videoId, language: $language")
            val result = lyricService.getTranslatedLyricByVideoIdAndLanguage(videoId, language).last()
            when (result) {
                is Resource.Success -> {
                    result.data?.let {
                        logger.debug("Found translated lyrics for videoId: $videoId, language: $language")
                        ResponseEntity.ok(it)
                    } ?: ResponseEntity.notFound().build()
                }
                is Resource.Error -> {
                    logger.error("Failed to get translated lyrics: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
    
    @PostMapping("/translated")
    suspend fun createTranslatedLyric(@RequestBody translatedLyricRequestDTO: TranslatedLyricRequestDTO): ResponseEntity<Any> {
        return withContext(ioDispatcher) {
            logger.debug("createTranslatedLyric --> Creating translated lyric")
            val result = lyricService.saveTranslatedLyric(translatedLyricRequestDTO).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("createTranslatedLyric --> Successfully created translated lyric")
                    ResponseEntity.status(HttpStatus.CREATED).body(result.data)
                }
                is Resource.Error -> {
                    logger.error("createTranslatedLyric --> Failed to create translated lyric: ${result.message}", result.exception)
                    val errorResponse = result.toErrorResponse()
                    ResponseEntity.status(HttpStatus.valueOf(errorResponse.code)).body(errorResponse)
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    // ========== Vote API Endpoints ==========
    
    @PostMapping("/vote")
    suspend fun voteLyric(@RequestBody voteDTO: VoteDTO): ResponseEntity<Any> {
        return withContext(ioDispatcher) {
            logger.debug("voteLyric --> Processing vote request for lyric id: ${voteDTO.id}")
            val result = lyricService.voteLyric(voteDTO).last()
            
            when (result) {
                is Resource.Success -> {
                    logger.debug("voteLyric --> Successfully processed vote for lyric id: ${voteDTO.id}")
                    ResponseEntity.ok(result.data)
                }
                is Resource.Error -> {
                    logger.error("voteLyric --> Failed to process vote: ${result.message}", result.exception)
                    val errorResponse = result.toErrorResponse()
                    ResponseEntity.status(HttpStatus.valueOf(errorResponse.code)).body(errorResponse)
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
    
    @PostMapping("/translated/vote")
    suspend fun voteTranslatedLyric(@RequestBody voteDTO: VoteDTO): ResponseEntity<Any> {
        return withContext(ioDispatcher) {
            logger.debug("voteTranslatedLyric --> Processing vote request for translated lyric id: ${voteDTO.id}")
            val result = lyricService.voteTranslatedLyric(voteDTO).last()
            
            when (result) {
                is Resource.Success -> {
                    logger.debug("voteTranslatedLyric --> Successfully processed vote for translated lyric id: ${voteDTO.id}")
                    ResponseEntity.ok(result.data)
                }
                is Resource.Error -> {
                    logger.error("voteTranslatedLyric --> Failed to process vote: ${result.message}", result.exception)
                    val errorResponse = result.toErrorResponse()
                    ResponseEntity.status(HttpStatus.valueOf(errorResponse.code)).body(errorResponse)
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
}
