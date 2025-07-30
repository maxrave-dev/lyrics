package org.simpmusic.lyrics.domain.repository

import kotlinx.coroutines.flow.Flow
import org.simpmusic.lyrics.domain.model.NotFoundTranslatedLyric
import org.simpmusic.lyrics.domain.model.Resource

/**
 * Repository interface for not found translated lyrics domain operations
 * This is a clean architecture pattern where repository interfaces live in the domain layer
 * but implementations live in the infrastructure layer
 */
interface NotFoundTranslatedLyricRepository {
    fun findById(id: String): Flow<Resource<NotFoundTranslatedLyric?>>
    fun findByVideoIdAndLanguage(videoId: String, language: String): Flow<Resource<NotFoundTranslatedLyric?>>
    fun findBySha256Hash(sha256hash: String): Flow<Resource<NotFoundTranslatedLyric?>>
    fun findAllOrderedByDate(limit: Int? = null, offset: Int? = null): Flow<Resource<List<NotFoundTranslatedLyric>>>
    fun save(notFoundTranslatedLyric: NotFoundTranslatedLyric): Flow<Resource<NotFoundTranslatedLyric>>
    fun deleteByVideoIdAndLanguage(videoId: String, language: String): Flow<Resource<Boolean>>
} 