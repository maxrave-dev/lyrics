package org.simpmusic.lyrics.infrastructure.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.simpmusic.lyrics.domain.model.Lyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * In-memory implementation of the LyricRepository
 * Used only for local development and testing
 */
@Repository
@Profile("local", "test")
@OptIn(ExperimentalUuidApi::class)
class InMemoryLyricRepository : LyricRepository {
    private val lyrics = ConcurrentHashMap<String, Lyric>()

    override fun findById(id: String): Flow<Resource<Lyric?>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(lyrics[id]))
    }.flowOn(Dispatchers.IO)

    override fun findByVideoId(videoId: String): Flow<Resource<List<Lyric>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(lyrics.values.filter { it.videoId == videoId }))
    }.flowOn(Dispatchers.IO)

    override fun findAll(): Flow<Resource<List<Lyric>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(lyrics.values.toList()))
    }.flowOn(Dispatchers.IO)

    override fun findBySongTitle(title: String): Flow<Resource<List<Lyric>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(lyrics.values.filter { it.songTitle.contains(title, ignoreCase = true) }))
    }.flowOn(Dispatchers.IO)

    override fun findByArtist(artist: String): Flow<Resource<List<Lyric>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(lyrics.values.filter { it.artistName.contains(artist, ignoreCase = true) }))
    }.flowOn(Dispatchers.IO)

    override fun search(keywords: String): Flow<Resource<List<Lyric>>> = flow {
        emit(Resource.Loading)
        val results = lyrics.values.filter { lyric ->
            lyric.songTitle.contains(keywords, ignoreCase = true) ||
            lyric.artistName.contains(keywords, ignoreCase = true) ||
            lyric.albumName.contains(keywords, ignoreCase = true) ||
            lyric.plainLyric.contains(keywords, ignoreCase = true) ||
            lyric.contributor.contains(keywords, ignoreCase = true)
        }
        emit(Resource.Success(results))
    }.flowOn(Dispatchers.IO)

    override fun save(lyric: Lyric): Flow<Resource<Lyric>> = flow {
        emit(Resource.Loading)
        lyrics[lyric.id.toString()] = lyric
        emit(Resource.Success(lyric))
    }.flowOn(Dispatchers.IO)

    override fun delete(id: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(lyrics.remove(id) != null))
    }.flowOn(Dispatchers.IO)
}
