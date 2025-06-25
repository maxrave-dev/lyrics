package org.simpmusic.lyrics.presentation.controller

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.simpmusic.lyrics.application.dto.LyricDTO
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

    @GetMapping
    suspend fun getAllLyrics(): ResponseEntity<List<LyricDTO>> {
        return withContext(ioDispatcher) {
            val result = lyricService.getAllLyrics().first()
            when (result) {
                is Resource.Success -> ResponseEntity.ok(result.data)
                is Resource.Error -> {
                    logger.error("Failed to get all lyrics: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @GetMapping("/{id}")
    suspend fun getLyricById(@PathVariable id: String): ResponseEntity<LyricDTO> {
        return withContext(ioDispatcher) {
            val result = lyricService.getLyricById(id).first()
            when (result) {
                is Resource.Success -> {
                    if (result.data != null) {
                        ResponseEntity.ok(result.data)
                    } else {
                        ResponseEntity.notFound().build()
                    }
                }
                is Resource.Error -> {
                    logger.error("Failed to get lyric by id: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @GetMapping("/search/title")
    suspend fun getLyricsBySongTitle(@RequestParam title: String): ResponseEntity<List<LyricDTO>> {
        return withContext(ioDispatcher) {
            val result = lyricService.getLyricsBySongTitle(title).first()
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
            val result = lyricService.getLyricsByArtist(artist).first()
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

    @PostMapping
    suspend fun createLyric(@RequestBody lyricDTO: LyricDTO): ResponseEntity<LyricDTO> {
        return withContext(ioDispatcher) {
            val result = lyricService.saveLyric(lyricDTO).first()
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

    @PutMapping("/{id}")
    suspend fun updateLyric(
        @PathVariable id: String,
        @RequestBody lyricDTO: LyricDTO
    ): ResponseEntity<LyricDTO> {
        return withContext(ioDispatcher) {
            val checkResult = lyricService.getLyricById(id).first()
            
            if (checkResult is Resource.Success && checkResult.data == null) {
                return@withContext ResponseEntity.notFound().build()
            }
            
            val result = lyricService.saveLyric(lyricDTO).first()
            when (result) {
                is Resource.Success -> ResponseEntity.ok(result.data)
                is Resource.Error -> {
                    logger.error("Failed to update lyric: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }

    @DeleteMapping("/{id}")
    suspend fun deleteLyric(@PathVariable id: String): ResponseEntity<Unit> {
        return withContext(ioDispatcher) {
            val result = lyricService.deleteLyric(id).first()
            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        ResponseEntity.noContent().build()
                    } else {
                        ResponseEntity.notFound().build()
                    }
                }
                is Resource.Error -> {
                    logger.error("Failed to delete lyric: ${result.message}", result.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
    
    /**
     * Admin endpoints for Appwrite management
     */
    @PostMapping("/admin/initialize")
    suspend fun initializeAppwrite(): ResponseEntity<Map<String, String>> {
        return withContext(ioDispatcher) {
            val result = lyricService.initializeAppwrite().first()
            when (result) {
                is Resource.Success -> {
                    ResponseEntity.ok(mapOf("status" to "success", "message" to result.data))
                }
                is Resource.Error -> {
                    logger.error("Failed to initialize Appwrite: ${result.message}", result.exception)
                    ResponseEntity.internalServerError().body(
                        mapOf("status" to "error", "message" to "Failed to initialize Appwrite: ${result.message}")
                    )
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
    
    @DeleteMapping("/admin/clear")
    suspend fun clearAllLyrics(): ResponseEntity<Map<String, String>> {
        return withContext(ioDispatcher) {
            val result = lyricService.clearAllLyrics().first()
            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        ResponseEntity.ok(mapOf("status" to "success", "message" to "All lyrics cleared successfully"))
                    } else {
                        ResponseEntity.internalServerError().body(
                            mapOf("status" to "error", "message" to "Failed to clear all lyrics")
                        )
                    }
                }
                is Resource.Error -> {
                    logger.error("Failed to clear all lyrics: ${result.message}", result.exception)
                    ResponseEntity.internalServerError().body(
                        mapOf("status" to "error", "message" to "Failed to clear all lyrics: ${result.message}")
                    )
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
    
    @PostMapping("/admin/rebuild")
    suspend fun rebuildDatabase(): ResponseEntity<Map<String, String>> {
        return withContext(ioDispatcher) {
            val result = lyricService.rebuildDatabase().first()
            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        ResponseEntity.ok(mapOf("status" to "success", "message" to "Database rebuilt successfully"))
                    } else {
                        ResponseEntity.internalServerError().body(
                            mapOf("status" to "error", "message" to "Failed to rebuild database")
                        )
                    }
                }
                is Resource.Error -> {
                    logger.error("Failed to rebuild database: ${result.message}", result.exception)
                    ResponseEntity.internalServerError().body(
                        mapOf("status" to "error", "message" to "Failed to rebuild database: ${result.message}")
                    )
                }
                is Resource.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).build()
            }
        }
    }
}
