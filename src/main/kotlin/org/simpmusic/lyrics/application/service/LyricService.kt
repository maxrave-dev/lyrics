package org.simpmusic.lyrics.application.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import org.simpmusic.lyrics.application.dto.LyricDTO
import org.simpmusic.lyrics.domain.model.Lyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.infrastructure.datasource.AppwriteDataSource
import org.simpmusic.lyrics.infrastructure.util.catchToResourceError
import org.simpmusic.lyrics.infrastructure.util.logCompletion
import org.simpmusic.lyrics.infrastructure.util.logEach
import org.simpmusic.lyrics.infrastructure.util.mapSuccess
import org.simpmusic.lyrics.infrastructure.util.mapSuccessNotNull
import org.simpmusic.lyrics.infrastructure.util.shareHot
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Application service implementing lyrics use cases
 */
@Service
class LyricService(
    private val lyricRepository: LyricRepository,
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
    
    fun getLyricsByVideoId(videoId: String): Flow<Resource<List<LyricDTO>>> {
        return lyricRepository.findByVideoId(videoId)
            .mapSuccess { lyrics -> lyrics.map { it.toDTO() } }
            .catchToResourceError()
    }
    
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
    
    fun saveLyric(lyricDTO: LyricDTO): Flow<Resource<LyricDTO>> {
        val lyric = lyricDTO.toEntity()
        return lyricRepository.save(lyric)
            .mapSuccess { it.toDTO() }
            .catchToResourceError()
    }
    
    fun deleteLyric(id: String): Flow<Resource<Boolean>> {
        return lyricRepository.delete(id)
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
}
