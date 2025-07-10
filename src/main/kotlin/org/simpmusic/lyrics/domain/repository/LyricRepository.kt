package org.simpmusic.lyrics.domain.repository

import kotlinx.coroutines.flow.Flow
import org.simpmusic.lyrics.domain.model.Lyric
import org.simpmusic.lyrics.domain.model.Resource

/**
 * Repository interface for lyrics domain operations
 * This is a clean architecture pattern where repository interfaces live in the domain layer
 * but implementations live in the infrastructure layer
 */
interface LyricRepository {
    fun findById(id: String): Flow<Resource<Lyric?>>
    fun findByVideoId(videoId: String): Flow<Resource<List<Lyric>>>
    fun findBySongTitle(title: String): Flow<Resource<List<Lyric>>>
    fun findByArtist(artist: String): Flow<Resource<List<Lyric>>>
    fun search(keywords: String): Flow<Resource<List<Lyric>>>
    fun findBySha256Hash(sha256hash: String): Flow<Resource<Lyric?>>
    fun save(lyric: Lyric): Flow<Resource<Lyric>>
    fun updateVote(id: String, voteIncrement: Int): Flow<Resource<Lyric>>
}
