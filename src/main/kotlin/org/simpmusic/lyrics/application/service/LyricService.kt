package org.simpmusic.lyrics.application.service

import io.appwrite.ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import org.simpmusic.lyrics.application.dto.LyricDTO
import org.simpmusic.lyrics.application.dto.NotFoundLyricDTO
import org.simpmusic.lyrics.application.dto.TranslatedLyricDTO
import org.simpmusic.lyrics.domain.model.Lyric
import org.simpmusic.lyrics.domain.model.NotFoundLyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.model.TranslatedLyric
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.domain.repository.NotFoundLyricRepository
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.simpmusic.lyrics.infrastructure.datasource.AppwriteDataSource
import org.simpmusic.lyrics.infrastructure.util.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Application service implementing lyrics use cases
 */
@Service
class LyricService(
    private val lyricRepository: LyricRepository,
    private val translatedLyricRepository: TranslatedLyricRepository,
    private val notFoundLyricRepository: NotFoundLyricRepository,
    private val appwriteDataSource: AppwriteDataSource,
    private val serviceScope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(LyricService::class.java)
    
    /**
     * Initialize Appwrite database and collections
     * Uses shareHot to make the flow hot and allow multiple collectors
     */
    fun initializeAppwrite(): Flow<Resource<String>> {
        return appwriteDataSource.initializeAppwrite()
            .logEach("Appwrite initialization")
            .logCompletion("Appwrite initialization completed")
            .catchToResourceError()
            .shareHot(serviceScope)
    }
    
    /**
     * Clear all lyrics from the database
     * Uses shareHot to make the flow hot and allow multiple collectors
     */
    fun clearAllLyrics(): Flow<Resource<Boolean>> {
        return appwriteDataSource.clearAllLyrics()
            .logEach("Clear all lyrics")
            .logCompletion("Clear all lyrics completed")
            .catchToResourceError()
            .shareHot(serviceScope)
    }
    
    /**
     * Rebuild the database from scratch
     * Uses shareHot to make the flow hot and allow multiple collectors
     */
    fun rebuildDatabase(): Flow<Resource<Boolean>> {
        return appwriteDataSource.rebuildDatabase()
            .logEach("Rebuild database")
            .logCompletion("Rebuild database completed")
            .catchToResourceError()
            .shareHot(serviceScope)
    }
    
    fun getLyricById(id: String): Flow<Resource<LyricDTO?>> {
        return lyricRepository.findById(id)
            .mapSuccessNotNull { it.toDTO() }
            .catchToResourceError()
    }
    
    fun getLyricsByVideoId(videoId: String): Flow<Resource<List<LyricDTO>>> = flow {
        emit(Resource.Loading)
        
                try {
            // First, try to find lyrics
            val lyricsResult = lyricRepository.findByVideoId(videoId).last()
            
            when (lyricsResult) {
                is Resource.Success -> {
                    if (lyricsResult.data.isEmpty()) {
                        // No lyrics found, check if already in notfound_lyrics
                        logger.debug("No lyrics found for videoId: $videoId, checking notfound_lyrics...")
                        
                        val notFoundResult = notFoundLyricRepository.findByVideoId(videoId).last()
                        
                        when (notFoundResult) {
                            is Resource.Success -> {
                                if (notFoundResult.data == null) {
                                    // Not in notfound_lyrics yet, save it
                                    logger.info("Adding videoId: $videoId to notfound_lyrics")
                                    val notFoundLyric = NotFoundLyric(
                                        videoId = videoId,
                                        addedDate = LocalDateTime.now()
                                    )
                                    
                                    // Save to notfound_lyrics
                                    val saveResult = notFoundLyricRepository.save(notFoundLyric).last()
                                    when (saveResult) {
                                        is Resource.Success -> {
                                            logger.info("Successfully saved NotFoundLyric for videoId: $videoId")
                                        }
                                        is Resource.Error -> {
                                            logger.warn("Failed to save NotFoundLyric for videoId: $videoId - ${saveResult.message}")
                                        }
                                        else -> {} // Loading state
                                    }
                                } else {
                                    logger.debug("VideoId: $videoId already exists in notfound_lyrics")
                                }
                            }
                            is Resource.Error -> {
                                logger.warn("Error checking notfound_lyrics for videoId: $videoId - ${notFoundResult.message}")
                            }
                            else -> {} // Loading state
                        }
                        
                        // Return empty list regardless of notfound_lyrics operation result
                        emit(Resource.Success(emptyList<LyricDTO>()))
                    } else {
                        // Lyrics found, return them
                        val lyricDTOs = lyricsResult.data.map { it.toDTO() }
                        emit(Resource.Success(lyricDTOs))
                    }
                }
                is Resource.Error -> {
                    emit(Resource.Error(lyricsResult.message, lyricsResult.exception))
                }
                else -> {} // Loading state
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error getting lyrics for videoId: $videoId", e))
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    
    fun getAllLyrics(): Flow<Resource<List<LyricDTO>>> {
        return lyricRepository.findAll()
            .mapSuccess { lyrics -> lyrics.map { it.toDTO() } }
            .catchToResourceError()
    }
    
    fun getLyricsBySongTitle(title: String): Flow<Resource<List<LyricDTO>>> {
        return lyricRepository.findBySongTitle(title)
            .mapSuccess { lyrics -> lyrics.map { it.toDTO() } }
            .catchToResourceError()
    }
    
    fun getLyricsByArtist(artist: String): Flow<Resource<List<LyricDTO>>> {
        return lyricRepository.findByArtist(artist)
            .mapSuccess { lyrics -> lyrics.map { it.toDTO() } }
            .catchToResourceError()
    }
    
    fun searchLyrics(keywords: String): Flow<Resource<List<LyricDTO>>> {
        return lyricRepository.search(keywords)
            .mapSuccess { lyrics -> lyrics.map { it.toDTO() } }
            .catchToResourceError()
    }
    
    fun saveLyric(lyricDTO: LyricDTO): Flow<Resource<LyricDTO>> = flow {
        emit(Resource.Loading)
        
        try {
            val lyric = lyricDTO.toEntity()
            
            // Check for duplicate data based on sha256hash
            logger.debug("saveLyric --> Checking for duplicate lyric with sha256hash: ${lyric.sha256hash}")
            val existingResult = lyricRepository.findBySha256Hash(lyric.sha256hash).last()
            
            when (existingResult) {
                is Resource.Success -> {
                    if (existingResult.data != null) {
                        // Lyric with this sha256hash already exists
                        logger.warn("saveLyric --> Duplicate lyric detected with sha256hash: ${lyric.sha256hash}")
                        emit(Resource.duplicateError("This lyrics already exists"))
                        return@flow
                    }
                }
                is Resource.Error -> {
                    logger.warn("saveLyric --> Error checking for duplicate lyric: ${existingResult.message}")
                    // Continue to save if unable to check for duplicates
                }
                else -> {} // Loading state
            }
            
            val saveResult = lyricRepository.save(lyric).last()
            
            when (saveResult) {
                is Resource.Success -> {
                    // When lyrics are successfully saved, remove from notfound_lyrics if exists
                    logger.debug("Lyric saved successfully, checking if videoId: ${lyricDTO.videoId} exists in notfound_lyrics")
                    
                    try {
                        val deleteResult = notFoundLyricRepository.deleteByVideoId(lyricDTO.videoId).last()
                        when (deleteResult) {
                            is Resource.Success -> {
                                if (deleteResult.data) {
                                    logger.info("Removed videoId: ${lyricDTO.videoId} from notfound_lyrics after adding lyrics")
                                }
                            }
                            is Resource.Error -> {
                                logger.warn("Failed to remove videoId: ${lyricDTO.videoId} from notfound_lyrics: ${deleteResult.message}")
                            }
                            else -> {} // Loading state
                        }
                    } catch (e: Exception) {
                        logger.warn("Exception while removing videoId: ${lyricDTO.videoId} from notfound_lyrics", e)
                    }
                    
                    emit(Resource.Success(saveResult.data.toDTO()))
                }
                is Resource.Error -> {
                    emit(Resource.Error(saveResult.message, saveResult.exception))
                }
                else -> {} // Loading state
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error saving lyric", e))
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    
    fun deleteLyric(id: String): Flow<Resource<Boolean>> {
        return lyricRepository.delete(id)
            .catchToResourceError()
    }
    
    // ========== TranslatedLyric Methods ==========
    
    fun getTranslatedLyricById(id: String): Flow<Resource<TranslatedLyricDTO?>> {
        return translatedLyricRepository.findById(id)
            .mapSuccessNotNull { it.toDTO() }
            .catchToResourceError()
    }
    
    fun getTranslatedLyricsByVideoId(videoId: String): Flow<Resource<List<TranslatedLyricDTO>>> {
        return translatedLyricRepository.findByVideoId(videoId)
            .mapSuccess { translatedLyrics -> translatedLyrics.map { it.toDTO() } }
            .catchToResourceError()
    }
    
    fun getTranslatedLyricByVideoIdAndLanguage(videoId: String, language: String): Flow<Resource<TranslatedLyricDTO?>> {
        return translatedLyricRepository.findByVideoIdAndLanguage(videoId, language)
            .mapSuccessNotNull { it.toDTO() }
            .catchToResourceError()
    }
    
    fun getTranslatedLyricsByLanguage(language: String): Flow<Resource<List<TranslatedLyricDTO>>> {
        return translatedLyricRepository.findByLanguage(language)
            .mapSuccess { translatedLyrics -> translatedLyrics.map { it.toDTO() } }
            .catchToResourceError()
    }
    
    fun getAllTranslatedLyrics(): Flow<Resource<List<TranslatedLyricDTO>>> {
        return translatedLyricRepository.findAll()
            .mapSuccess { translatedLyrics -> translatedLyrics.map { it.toDTO() } }
            .catchToResourceError()
    }
    
    fun saveTranslatedLyric(translatedLyricDTO: TranslatedLyricDTO): Flow<Resource<TranslatedLyricDTO>> = flow {
        emit(Resource.Loading)
        
        try {
            val translatedLyric = translatedLyricDTO.toEntity()
            
            // Check for duplicate data based on sha256hash
            logger.debug("saveTranslatedLyric --> Checking for duplicate translated lyric with sha256hash: ${translatedLyric.sha256hash}")
            val existingResult = translatedLyricRepository.findBySha256Hash(translatedLyric.sha256hash).last()
            
            when (existingResult) {
                is Resource.Success -> {
                    if (existingResult.data != null) {
                        // Translated lyric with this sha256hash already exists
                        logger.warn("saveTranslatedLyric --> Duplicate translated lyric detected with sha256hash: ${translatedLyric.sha256hash}")
                        emit(Resource.duplicateError("This translated lyrics already exists"))
                        return@flow
                    }
                }
                is Resource.Error -> {
                    logger.warn("saveTranslatedLyric --> Error checking for duplicate translated lyric: ${existingResult.message}")
                    // Continue to save if unable to check for duplicates
                }
                else -> {} // Loading state
            }
            
            val saveResult = translatedLyricRepository.save(translatedLyric).last()
            
            when (saveResult) {
                is Resource.Success -> {
                    emit(Resource.Success(saveResult.data.toDTO()))
                }
                is Resource.Error -> {
                    emit(Resource.Error(saveResult.message, saveResult.exception))
                }
                else -> {} // Loading state
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error saving translated lyric", e))
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    
    fun deleteTranslatedLyric(id: String): Flow<Resource<Boolean>> {
        return translatedLyricRepository.delete(id)
            .catchToResourceError()
    }
    
    // ========== NotFoundLyric Methods ==========
    
    fun getNotFoundLyricById(id: String): Flow<Resource<NotFoundLyricDTO?>> {
        return notFoundLyricRepository.findById(id)
            .mapSuccessNotNull { it.toDTO() }
            .catchToResourceError()
    }
    
    fun getNotFoundLyricByVideoId(videoId: String): Flow<Resource<NotFoundLyricDTO?>> {
        return notFoundLyricRepository.findByVideoId(videoId)
            .mapSuccessNotNull { it.toDTO() }
            .catchToResourceError()
    }
    
    fun getAllNotFoundLyrics(): Flow<Resource<List<NotFoundLyricDTO>>> {
        return notFoundLyricRepository.findAll()
            .mapSuccess { notFoundLyrics -> notFoundLyrics.map { it.toDTO() } }
            .catchToResourceError()
    }
    
    fun getAllNotFoundLyricsOrderedByDate(): Flow<Resource<List<NotFoundLyricDTO>>> {
        return notFoundLyricRepository.findAllOrderedByDate()
            .mapSuccess { notFoundLyrics -> notFoundLyrics.map { it.toDTO() } }
            .catchToResourceError()
    }
    
    fun saveNotFoundLyric(notFoundLyricDTO: NotFoundLyricDTO): Flow<Resource<NotFoundLyricDTO>> = flow {
        emit(Resource.Loading)
        
        try {
            val notFoundLyric = notFoundLyricDTO.toEntity()
            
            // Check for duplicate data based on videoId
            logger.debug("saveNotFoundLyric --> Checking for duplicate notfound lyric with videoId: ${notFoundLyric.videoId}")
            val existingResult = notFoundLyricRepository.findByVideoId(notFoundLyric.videoId).last()
            
            when (existingResult) {
                is Resource.Success -> {
                    if (existingResult.data != null) {
                        // NotFound lyric with this videoId already exists
                        logger.warn("saveNotFoundLyric --> Duplicate notfound lyric detected with videoId: ${notFoundLyric.videoId}")
                        emit(Resource.duplicateError("VideoId already exists in notfound lyrics list"))
                        return@flow
                    }
                }
                is Resource.Error -> {
                    logger.warn("saveNotFoundLyric --> Error checking for duplicate notfound lyric: ${existingResult.message}")
                    // Continue to save if unable to check for duplicates
                }
                else -> {} // Loading state
            }
            
            val saveResult = notFoundLyricRepository.save(notFoundLyric).last()
            
            when (saveResult) {
                is Resource.Success -> {
                    emit(Resource.Success(saveResult.data.toDTO()))
                }
                is Resource.Error -> {
                    emit(Resource.Error(saveResult.message, saveResult.exception))
                }
                else -> {} // Loading state
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error saving notfound lyric", e))
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    
    fun deleteNotFoundLyric(id: String): Flow<Resource<Boolean>> {
        return notFoundLyricRepository.delete(id)
            .catchToResourceError()
    }
    
    fun deleteNotFoundLyricByVideoId(videoId: String): Flow<Resource<Boolean>> {
        return notFoundLyricRepository.deleteByVideoId(videoId)
            .catchToResourceError()
    }
    
    /**
     * Internal method to check if a videoId exists in notfound_lyrics
     * This can be useful for monitoring and debugging purposes
     */
    internal fun isVideoInNotFoundList(videoId: String): Flow<Resource<Boolean>> {
        return notFoundLyricRepository.findByVideoId(videoId)
            .mapSuccess { it != null }
            .catchToResourceError()
    }
    
    private fun Lyric.toDTO(): LyricDTO {
        return LyricDTO(
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
    
    private fun LyricDTO.toEntity(): Lyric {
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
    
    // ========== TranslatedLyric Conversion Methods ==========
    
    private fun TranslatedLyric.toDTO(): TranslatedLyricDTO {
        return TranslatedLyricDTO(
            videoId = videoId,
            translatedLyric = translatedLyric,
            language = language,
            vote = vote,
            contributor = contributor,
            contributorEmail = contributorEmail
        )
    }
    
    private fun TranslatedLyricDTO.toEntity(): TranslatedLyric {
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
    
    // ========== NotFoundLyric Conversion Methods ==========
    
    private fun NotFoundLyric.toDTO(): NotFoundLyricDTO {
        return NotFoundLyricDTO(
            videoId = videoId,
            addedDate = addedDate
        )
    }
    
    private fun NotFoundLyricDTO.toEntity(): NotFoundLyric {
        return NotFoundLyric(
            videoId = videoId,
            addedDate = addedDate
        )
    }
}
