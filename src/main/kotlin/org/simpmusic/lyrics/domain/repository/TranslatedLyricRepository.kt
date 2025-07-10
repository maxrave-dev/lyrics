package org.simpmusic.lyrics.domain.repository

import kotlinx.coroutines.flow.Flow
import org.simpmusic.lyrics.domain.model.TranslatedLyric
import org.simpmusic.lyrics.domain.model.Resource

/**
 * Repository interface for translated lyrics domain operations
 * This is a clean architecture pattern where repository interfaces live in the domain layer
 * but implementations live in the infrastructure layer
 */
interface TranslatedLyricRepository {
    fun findById(id: String): Flow<Resource<TranslatedLyric?>>
    fun findByVideoId(videoId: String, limit: Int? = null, offset: Int? = null): Flow<Resource<List<TranslatedLyric>>>
    fun findByVideoIdAndLanguage(videoId: String, language: String): Flow<Resource<TranslatedLyric?>>
    fun findByLanguage(language: String, limit: Int? = null, offset: Int? = null): Flow<Resource<List<TranslatedLyric>>>
    fun findBySha256Hash(sha256hash: String): Flow<Resource<TranslatedLyric?>>
    fun save(translatedLyric: TranslatedLyric): Flow<Resource<TranslatedLyric>>
    fun updateVote(id: String, voteIncrement: Int): Flow<Resource<TranslatedLyric>>
} 