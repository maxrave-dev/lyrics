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
    suspend fun getLyricsByVideoId(@PathVariable videoId: String): ResponseEntity<List<LyricDTO>> {
        return withContext(ioDispatcher) {
            logger.debug("Getting lyrics for videoId: $videoId")
            val result = lyricService.getLyricsByVideoId(videoId).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("Found ${result.data.size} lyrics for videoId: $videoId")
                    ResponseEntity.ok(result.data)
                }
                is Resource.Error -> {
                    logger.error("Failed to get lyrics by videoId: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @GetMapping("/search/title")
    suspend fun getLyricsBySongTitle(@RequestParam title: String): ResponseEntity<List<LyricDTO>> {
        return withContext(ioDispatcher) {
            val result = lyricService.getLyricsBySongTitle(title).last()
            when (result) {
                is Resource.Success -> ResponseEntity.ok(result.data)
                is Resource.Error -> {
                    logger.error("Failed to get lyrics by title: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @GetMapping("/search/artist")
    suspend fun getLyricsByArtist(@RequestParam artist: String): ResponseEntity<List<LyricDTO>> {
        return withContext(ioDispatcher) {
            val result = lyricService.getLyricsByArtist(artist).last()
            when (result) {
                is Resource.Success -> ResponseEntity.ok(result.data)
                is Resource.Error -> {
                    logger.error("Failed to get lyrics by artist: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @GetMapping("/search")
    suspend fun searchLyrics(@RequestParam q: String): ResponseEntity<List<LyricDTO>> {
        return withContext(ioDispatcher) {
            logger.debug("Searching lyrics with keywords: $q")
            val result = lyricService.searchLyrics(q).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("Found ${result.data.size} lyrics for search: $q")
                    ResponseEntity.ok(result.data)
                }
                is Resource.Error -> {
                    logger.error("Failed to search lyrics: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @PostMapping
    suspend fun createLyric(@RequestBody lyricDTO: LyricDTO): ResponseEntity<LyricDTO> {
        return withContext(ioDispatcher) {
            logger.debug("Creating Lyric")
            val result = lyricService.saveLyric(lyricDTO).last()
            when (result) {
                is Resource.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.data)
                is Resource.Error -> {
                    logger.error("Failed to create lyric: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
    
    // ========== TranslatedLyrics API Endpoints ==========
    
    @GetMapping("/translated/{videoId}")
    suspend fun getTranslatedLyricsByVideoId(@PathVariable videoId: String): ResponseEntity<List<TranslatedLyricDTO>> {
        return withContext(ioDispatcher) {
            logger.debug("Getting translated lyrics for videoId: $videoId")
            val result = lyricService.getTranslatedLyricsByVideoId(videoId).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("Found ${result.data.size} translated lyrics for videoId: $videoId")
                    ResponseEntity.ok(result.data)
                }
                is Resource.Error -> {
                    logger.error("Failed to get translated lyrics by videoId: ${result.message}", result.exception)
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
    ): ResponseEntity<TranslatedLyricDTO> {
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
    
    @GetMapping("/translated/language/{language}")
    suspend fun getTranslatedLyricsByLanguage(@PathVariable language: String): ResponseEntity<List<TranslatedLyricDTO>> {
        return withContext(ioDispatcher) {
            logger.debug("Getting translated lyrics for language: $language")
            val result = lyricService.getTranslatedLyricsByLanguage(language).last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("Found ${result.data.size} translated lyrics for language: $language")
                    ResponseEntity.ok(result.data)
                }
                is Resource.Error -> {
                    logger.error("Failed to get translated lyrics by language: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
    
    @GetMapping("/translated")
    suspend fun getAllTranslatedLyrics(): ResponseEntity<List<TranslatedLyricDTO>> {
        return withContext(ioDispatcher) {
            logger.debug("Getting all translated lyrics")
            val result = lyricService.getAllTranslatedLyrics().last()
            when (result) {
                is Resource.Success -> {
                    logger.debug("Found ${result.data.size} translated lyrics")
                    ResponseEntity.ok(result.data)
                }
                is Resource.Error -> {
                    logger.error("Failed to get all translated lyrics: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
    
    @PostMapping("/translated")
    suspend fun createTranslatedLyric(@RequestBody translatedLyricDTO: TranslatedLyricDTO): ResponseEntity<TranslatedLyricDTO> {
        return withContext(ioDispatcher) {
            logger.debug("Creating translated lyric")
            val result = lyricService.saveTranslatedLyric(translatedLyricDTO).last()
            when (result) {
                is Resource.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.data)
                is Resource.Error -> {
                    logger.error("Failed to create translated lyric: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
}
