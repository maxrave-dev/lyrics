package org.simpmusic.lyrics.application.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import org.simpmusic.lyrics.application.dto.LyricDTO
import org.simpmusic.lyrics.application.dto.TranslatedLyricDTO
import org.simpmusic.lyrics.application.dto.NotFoundLyricDTO
import org.simpmusic.lyrics.domain.model.Lyric
import org.simpmusic.lyrics.domain.model.TranslatedLyric
import org.simpmusic.lyrics.domain.model.NotFoundLyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.simpmusic.lyrics.domain.repository.NotFoundLyricRepository
import org.simpmusic.lyrics.infrastructure.datasource.AppwriteDataSource
import org.simpmusic.lyrics.infrastructure.util.catchToResourceError
import org.simpmusic.lyrics.infrastructure.util.logCompletion
import org.simpmusic.lyrics.infrastructure.util.logEach
import org.simpmusic.lyrics.infrastructure.util.mapSuccess
import org.simpmusic.lyrics.infrastructure.util.mapSuccessNotNull
import org.simpmusic.lyrics.infrastructure.util.shareHot
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    
    @OptIn(ExperimentalUuidApi::class)
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
                                        id = Uuid.random(),
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
    
    fun saveTranslatedLyric(translatedLyricDTO: TranslatedLyricDTO): Flow<Resource<TranslatedLyricDTO>> {
        val translatedLyric = translatedLyricDTO.toEntity()
        return translatedLyricRepository.save(translatedLyric)
            .mapSuccess { it.toDTO() }
            .catchToResourceError()
    }
    
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
    
    fun saveNotFoundLyric(notFoundLyricDTO: NotFoundLyricDTO): Flow<Resource<NotFoundLyricDTO>> {
        val notFoundLyric = notFoundLyricDTO.toEntity()
        return notFoundLyricRepository.save(notFoundLyric)
            .mapSuccess { it.toDTO() }
            .catchToResourceError()
    }
    
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
    
    @OptIn(ExperimentalUuidApi::class)
    private fun Lyric.toDTO(): LyricDTO {
        return LyricDTO(
            id = id.toString(),
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
    
    @OptIn(ExperimentalUuidApi::class)
    private fun LyricDTO.toEntity(): Lyric {
        return Lyric(
            id = Uuid.parse(id),
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
    
    @OptIn(ExperimentalUuidApi::class)
    private fun TranslatedLyric.toDTO(): TranslatedLyricDTO {
        return TranslatedLyricDTO(
            id = id.toString(),
            videoId = videoId,
            translatedLyric = translatedLyric,
            language = language,
            vote = vote,
            contributor = contributor,
            contributorEmail = contributorEmail
        )
    }
    
    @OptIn(ExperimentalUuidApi::class)
    private fun TranslatedLyricDTO.toEntity(): TranslatedLyric {
        return TranslatedLyric(
            id = Uuid.parse(id),
            videoId = videoId,
            translatedLyric = translatedLyric,
            language = language,
            vote = vote,
            contributor = contributor,
            contributorEmail = contributorEmail
        )
    }
    
    // ========== NotFoundLyric Conversion Methods ==========
    
    @OptIn(ExperimentalUuidApi::class)
    private fun NotFoundLyric.toDTO(): NotFoundLyricDTO {
        return NotFoundLyricDTO(
            id = id.toString(),
            videoId = videoId,
            addedDate = addedDate
        )
    }
    
    @OptIn(ExperimentalUuidApi::class)
    private fun NotFoundLyricDTO.toEntity(): NotFoundLyric {
        return NotFoundLyric(
            id = Uuid.parse(id),
            videoId = videoId,
            addedDate = addedDate
        )
    }
}
