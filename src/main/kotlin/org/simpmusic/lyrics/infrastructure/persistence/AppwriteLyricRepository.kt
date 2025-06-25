package org.simpmusic.lyrics.infrastructure.persistence

import io.appwrite.ID
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.Document
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import org.simpmusic.lyrics.domain.model.Lyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Appwrite implementation of the LyricRepository
 */
@Repository("appwriteLyricRepositoryImpl")
@OptIn(ExperimentalUuidApi::class)
class AppwriteLyricRepository(
    private val databases: Databases,
    @Qualifier("databaseId") private val databaseId: String,
    @Qualifier("lyricsCollectionId") private val collectionId: String
) : LyricRepository {

    override fun findById(id: String): Flow<Resource<Lyric?>> = flow<Resource<Lyric?>> {
        emit(Resource.Loading)
        val document = databases.getDocument(
            databaseId = databaseId,
            collectionId = collectionId,
            documentId = id
        )
        emit(Resource.Success(documentToLyric(document)))
    }.catch { e ->
        if (e is AppwriteException) {
            if (e.code == 404) {
                emit(Resource.Success<Lyric?>(null))
            } else {
                emit(Resource.Error("Failed to find lyric: ${e.message}", e as? Exception))
            }
        } else {
            emit(Resource.Error("Unknown error: ${e.message}", e as? Exception))
        }
    }.flowOn(Dispatchers.IO)

    override fun findAll(): Flow<Resource<List<Lyric>>> = flow {
        emit(Resource.Loading)
        val documents = databases.listDocuments(
            databaseId = databaseId,
            collectionId = collectionId
        )
        emit(Resource.Success(documents.documents.map { documentToLyric(it) }))
    }.catch { e ->
        if (e is AppwriteException) {
            emit(Resource.Error("Failed to find lyrics: ${e.message}", e as? Exception))
        } else {
            emit(Resource.Error("Unknown error: ${e.message}", e as? Exception))
        }
    }.flowOn(Dispatchers.IO)

    override fun findBySongTitle(title: String): Flow<Resource<List<Lyric>>> = flow {
        emit(Resource.Loading)
        val documents = databases.listDocuments(
            databaseId = databaseId,
            collectionId = collectionId,
            queries = listOf("songTitle CONTAINS \"$title\"")
        )
        emit(Resource.Success(documents.documents.map { documentToLyric(it) }))
    }.catch { e ->
        if (e is AppwriteException) {
            emit(Resource.Error("Failed to find lyrics by title: ${e.message}", e as? Exception))
        } else {
            emit(Resource.Error("Unknown error: ${e.message}", e as? Exception))
        }
    }.flowOn(Dispatchers.IO)

    override fun findByArtist(artist: String): Flow<Resource<List<Lyric>>> = flow {
        emit(Resource.Loading)
        val documents = databases.listDocuments(
            databaseId = databaseId,
            collectionId = collectionId,
            queries = listOf("artistName CONTAINS \"$artist\"")
        )
        emit(Resource.Success(documents.documents.map { documentToLyric(it) }))
    }.catch { e ->
        if (e is AppwriteException) {
            emit(Resource.Error("Failed to find lyrics by artist: ${e.message}", e as? Exception))
        } else {
            emit(Resource.Error("Unknown error: ${e.message}", e as? Exception))
        }
    }.flowOn(Dispatchers.IO)

    override fun save(lyric: Lyric): Flow<Resource<Lyric>> = flow {
        emit(Resource.Loading)
        val data = mapOf(
            "id" to lyric.id.toString(),
            "videoId" to lyric.videoId,
            "songTitle" to lyric.songTitle,
            "artistName" to lyric.artistName,
            "albumName" to lyric.albumName,
            "durationSeconds" to lyric.durationSeconds,
            "plainLyric" to lyric.plainLyric,
            "syncedLyrics" to lyric.syncedLyrics,
            "richSyncLyrics" to lyric.richSyncLyrics,
            "vote" to lyric.vote,
            "contributor" to lyric.contributor,
            "contributorEmail" to lyric.contributorEmail
        )
        
        val document = try {
            // Try to get document first
            databases.getDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = lyric.id.toString()
            )
            
            // Update if exists
            databases.updateDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = lyric.id.toString(),
                data = data
            )
        } catch (e: AppwriteException) {
            // Create if doesn't exist
            databases.createDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = ID.unique(),
                data = data
            )
        }
        
        emit(Resource.Success(documentToLyric(document)))
    }.catch { e ->
        if (e is AppwriteException) {
            emit(Resource.Error("Failed to save lyric: ${e.message}", e as? Exception))
        } else {
            emit(Resource.Error("Unknown error: ${e.message}", e as? Exception))
        }
    }.flowOn(Dispatchers.IO)

    override fun delete(id: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        databases.deleteDocument(
            databaseId = databaseId,
            collectionId = collectionId,
            documentId = id
        )
        emit(Resource.Success(true))
    }.catch { e ->
        if (e is AppwriteException) {
            if (e.code == 404) {
                emit(Resource.Success(false))
            } else {
                emit(Resource.Error("Failed to delete lyric: ${e.message}", e as? Exception))
            }
        } else {
            emit(Resource.Error("Unknown error: ${e.message}", e as? Exception))
        }
    }.flowOn(Dispatchers.IO)
    
    @OptIn(ExperimentalUuidApi::class)
    private fun documentToLyric(document: Document<Map<String, Any>>): Lyric {
        return Lyric(
            id = Uuid.parse(document.data["id"].toString()),
            videoId = document.data["videoId"].toString(),
            songTitle = document.data["songTitle"].toString(),
            artistName = document.data["artistName"].toString(),
            albumName = document.data["albumName"].toString(),
            durationSeconds = document.data["durationSeconds"].toString().toInt(),
            plainLyric = document.data["plainLyric"].toString(),
            syncedLyrics = document.data["syncedLyrics"].toString(),
            richSyncLyrics = document.data["richSyncLyrics"].toString(),
            vote = document.data["vote"].toString().toInt(),
            contributor = document.data["contributor"].toString(),
            contributorEmail = document.data["contributorEmail"].toString()
        )
    }
} 