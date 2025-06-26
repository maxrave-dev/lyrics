package org.simpmusic.lyrics.infrastructure.persistence

import io.appwrite.ID
import io.appwrite.Query
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
import org.slf4j.LoggerFactory
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
    
    private val logger = LoggerFactory.getLogger(AppwriteLyricRepository::class.java)

    override fun findById(id: String): Flow<Resource<Lyric?>> = flow {
        logger.debug("findById started for id: $id")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.getDocument for id: $id")
            databases.getDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = id
            )
        }.fold(
            onSuccess = { document ->
                logger.debug("Successfully found document for id: $id")
                emit(Resource.Success(documentToLyric(document)))
            },
            onFailure = { e ->
                e.printStackTrace()
                logger.debug("Failed to find document for id: $id, error: ${e.message}")
                if (e is AppwriteException && e.code == 404) {
                    logger.debug("Document not found (404) for id: $id")
                    emit(Resource.Success<Lyric?>(null))
                } else {
                    logger.error("Error finding lyric by id: $id", e)
                    emit(Resource.Error("Failed to find lyric: ${e.message}", e as? Exception))
                }
            }
        )
        logger.debug("findById completed for id: $id")
    }.flowOn(Dispatchers.IO)

    override fun findAll(): Flow<Resource<List<Lyric>>> = flow {
        logger.debug("findAll started")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.listDocuments")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId
            )
        }.fold(
            onSuccess = { documents ->
                logger.debug("Successfully found ${documents.documents.size} documents")
                emit(Resource.Success(documents.documents.map { documentToLyric(it) }))
            },
            onFailure = { e ->
                e.printStackTrace()
                logger.error("Error finding all lyrics", e)
                emit(Resource.Error("Failed to find lyrics: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findAll completed")
    }.flowOn(Dispatchers.IO)

    override fun findBySongTitle(title: String): Flow<Resource<List<Lyric>>> = flow {
        logger.debug("findBySongTitle started for title: $title")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.listDocuments with title query: $title")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId,
                queries = listOf(Query.contains("songTitle", title))
            )
        }.fold(
            onSuccess = { documents ->
                logger.debug("Successfully found ${documents.documents.size} documents for title: $title")
                emit(Resource.Success(documents.documents.map { documentToLyric(it) }))
            },
            onFailure = { e ->
                e.printStackTrace()
                logger.error("Error finding lyrics by title: $title", e)
                emit(Resource.Error("Failed to find lyrics by title: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findBySongTitle completed for title: $title")
    }.flowOn(Dispatchers.IO)

    override fun findByArtist(artist: String): Flow<Resource<List<Lyric>>> = flow {
        logger.debug("findByArtist started for artist: $artist")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.listDocuments with artist query: $artist")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId,
                queries = listOf(Query.contains("artistName", artist))
            )
        }.fold(
            onSuccess = { documents ->
                logger.debug("Successfully found ${documents.documents.size} documents for artist: $artist")
                emit(Resource.Success(documents.documents.map { documentToLyric(it) }))
            },
            onFailure = { e ->
                e.printStackTrace()
                logger.error("Error finding lyrics by artist: $artist", e)
                emit(Resource.Error("Failed to find lyrics by artist: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findByArtist completed for artist: $artist")
    }.flowOn(Dispatchers.IO)

    override fun findByVideoId(videoId: String): Flow<Resource<List<Lyric>>> = flow {
        logger.debug("findByVideoId started for videoId: $videoId")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.listDocuments with videoId query: $videoId")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId,
                queries = listOf(Query.equal("videoId", videoId))
            )
        }.fold(
            onSuccess = { documents ->
                logger.debug("Successfully found ${documents.documents.size} documents for videoId: $videoId")
                emit(Resource.Success(documents.documents.map { documentToLyric(it) }))
            },
            onFailure = { e ->
                e.printStackTrace()
                logger.error("Error finding lyrics by videoId: $videoId", e)
                emit(Resource.Error("Failed to find lyrics by videoId: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findByVideoId completed for videoId: $videoId")
    }.flowOn(Dispatchers.IO)

    override fun search(keywords: String): Flow<Resource<List<Lyric>>> = flow {
        logger.debug("search started for keywords: $keywords")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.listDocuments with full-text search query: $keywords")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId,
                queries = listOf(
                    Query.or(listOf(
                        Query.search("songTitle", keywords),
                        Query.search("artistName", keywords)
                    ))
                )
            )
        }.fold(
            onSuccess = { documents ->
                logger.debug("Successfully found ${documents.documents.size} documents for search: $keywords")
                emit(Resource.Success(documents.documents.map { documentToLyric(it) }))
            },
            onFailure = { e ->
                e.printStackTrace()
                logger.error("Error searching lyrics with keywords: $keywords", e)
                emit(Resource.Error("Failed to search lyrics: ${e.message}", e as? Exception))
            }
        )
        logger.debug("search completed for keywords: $keywords")
    }.flowOn(Dispatchers.IO)

    override fun save(lyric: Lyric): Flow<Resource<Lyric>> = flow {
        logger.debug("save started for lyric id: ${lyric.id}")
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
        logger.debug("Prepared data for save: ${data.keys}")
        
        runCatching {
            logger.debug("Checking if document exists for id: ${lyric.id}")
            
            // Try to get existing document first - simple approach
            var documentExists = false
            try {
                databases.getDocument(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    documentId = lyric.id.toString()
                )
                documentExists = true
                logger.debug("Document exists for id: ${lyric.id}, will update")
            } catch (e: AppwriteException) {
                if (e.code == 404) {
                    documentExists = false
                    logger.debug("Document doesn't exist for id: ${lyric.id}, will create")
                } else {
                    logger.error("Error checking document existence for id: ${lyric.id}", e)
                    throw e
                }
            }
            
            if (documentExists) {
                logger.debug("Updating existing document for id: ${lyric.id}")
                databases.updateDocument(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    documentId = lyric.id.toString(),
                    data = data
                )
            } else {
                logger.debug("Creating new document for id: ${lyric.id}")
                databases.createDocument(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    documentId = lyric.id.toString(),
                    data = data
                )
            }
        }.fold(
            onSuccess = { document ->
                logger.debug("Successfully saved lyric with id: ${lyric.id}")
                emit(Resource.Success(documentToLyric(document)))
            },
            onFailure = { e ->
                logger.error("Error saving lyric with id: ${lyric.id}", e)
                e.printStackTrace()
                emit(Resource.Error("Failed to save lyric: ${e.message}", e as? Exception))
            }
        )
        logger.debug("save completed for lyric id: ${lyric.id}")
    }.flowOn(Dispatchers.IO)

    override fun delete(id: String): Flow<Resource<Boolean>> = flow {
        logger.debug("delete started for id: $id")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.deleteDocument for id: $id")
            databases.deleteDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = id
            )
        }.fold(
            onSuccess = {
                logger.debug("Successfully deleted document for id: $id")
                emit(Resource.Success(true))
            },
            onFailure = { e ->
                e.printStackTrace()
                logger.debug("Failed to delete document for id: $id, error: ${e.message}")
                if (e is AppwriteException && e.code == 404) {
                    logger.debug("Document not found (404) for delete id: $id")
                    emit(Resource.Success(false))
                } else {
                    logger.error("Error deleting lyric with id: $id", e)
                    emit(Resource.Error("Failed to delete lyric: ${e.message}", e as? Exception))
                }
            }
        )
        logger.debug("delete completed for id: $id")
    }.flowOn(Dispatchers.IO)
    
    @OptIn(ExperimentalUuidApi::class)
    private fun documentToLyric(document: Document<Map<String, Any>>): Lyric {
        logger.debug("Converting document to Lyric: ${document.id}")
        logger.debug("Document data keys: ${document.data.keys}")
        logger.debug("Document data: $document.data")
        
        runCatching {
            val lyric = Lyric(
                id = Uuid.parse(document.data["id"].toString()),
                videoId = document.data["videoId"].toString(),
                songTitle = document.data["songTitle"].toString(),
                artistName = document.data["artistName"].toString(),
                albumName = document.data["albumName"].toString(),
                durationSeconds = document.data["durationSeconds"].toString().toInt(),
                plainLyric = document.data["plainLyric"].toString(),
                syncedLyrics = document.data["syncedLyrics"]?.toString(),
                richSyncLyrics = document.data["richSyncLyrics"]?.toString(),
                vote = document.data["vote"].toString().toInt(),
                contributor = document.data["contributor"].toString(),
                contributorEmail = document.data["contributorEmail"].toString()
            )
            logger.debug("Successfully converted document to Lyric: ${lyric.id}")
            return lyric
        }.getOrElse { e ->
            logger.error("Error converting document to Lyric", e)
            throw e
        }
    }
} 