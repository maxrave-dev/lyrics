package org.simpmusic.lyrics.domain.repository

import kotlinx.coroutines.flow.Flow
import org.simpmusic.lyrics.domain.model.NotFoundLyric
import org.simpmusic.lyrics.domain.model.Resource

/**
 * Repository interface for not found lyrics domain operations
 * This is a clean architecture pattern where repository interfaces live in the domain layer
 * but implementations live in the infrastructure layer
 */
interface NotFoundLyricRepository {
    fun findById(id: String): Flow<Resource<NotFoundLyric?>>
    fun findByVideoId(videoId: String): Flow<Resource<NotFoundLyric?>>
    fun findAllOrderedByDate(): Flow<Resource<List<NotFoundLyric>>>
    fun save(notFoundLyric: NotFoundLyric): Flow<Resource<NotFoundLyric>>
    fun deleteByVideoId(videoId: String): Flow<Resource<Boolean>>
} 