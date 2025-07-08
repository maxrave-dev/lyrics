package org.simpmusic.lyrics.infrastructure.persistence

import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.Document
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.simpmusic.lyrics.domain.model.NotFoundLyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.NotFoundLyricRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


/**
 * Appwrite implementation of the NotFoundLyricRepository
 */
@Repository("appwriteNotFoundLyricRepositoryImpl")
class AppwriteNotFoundLyricRepository(
    private val databases: Databases,
    @Qualifier("databaseId") private val databaseId: String,
    @Qualifier("notFoundLyricsCollectionId") private val collectionId: String
) : NotFoundLyricRepository {
    
    private val logger = LoggerFactory.getLogger(AppwriteNotFoundLyricRepository::class.java)

    override fun findById(id: String): Flow<Resource<NotFoundLyric?>> = flow {
        logger.debug("findById started for id: $id")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.getDocument for notfound lyric id: $id")
            databases.getDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = id
            )
        }.fold(
            onSuccess = { document ->
                logger.debug("Successfully found notfound lyric document for id: $id")
                emit(Resource.Success(documentToNotFoundLyric(document)))
            },
            onFailure = { e ->
                logger.debug("Failed to find notfound lyric document for id: $id, error: ${e.message}")
                if (e is AppwriteException && e.code == 404) {
                    logger.debug("NotFound lyric document not found (404) for id: $id")
                    emit(Resource.Success<NotFoundLyric?>(null))
                } else {
                    logger.error("Error finding notfound lyric by id: $id", e)
                    emit(Resource.Error("Failed to find notfound lyric: ${e.message}", e as? Exception))
                }
            }
        )
        logger.debug("findById completed for notfound lyric id: $id")
    }.flowOn(Dispatchers.IO)

    override fun findByVideoId(videoId: String): Flow<Resource<NotFoundLyric?>> = flow {
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
                val notFoundLyric = documents.documents.firstOrNull()?.let { documentToNotFoundLyric(it) }
                logger.debug("Found notfound lyric for videoId: $videoId: ${notFoundLyric != null}")
                emit(Resource.Success(notFoundLyric))
            },
            onFailure = { e ->
                logger.error("Error finding notfound lyric by videoId: $videoId", e)
                emit(Resource.Error("Failed to find notfound lyric by videoId: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findByVideoId completed for videoId: $videoId")
    }.flowOn(Dispatchers.IO)

    override fun findAll(): Flow<Resource<List<NotFoundLyric>>> = flow {
        logger.debug("findAll started for notfound lyrics")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.listDocuments for all notfound lyrics")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId
            )
        }.fold(
            onSuccess = { documents ->
                logger.debug("Successfully found ${documents.documents.size} notfound lyrics")
                emit(Resource.Success(documents.documents.map { documentToNotFoundLyric(it) }))
            },
            onFailure = { e ->
                logger.error("Error finding all notfound lyrics", e)
                emit(Resource.Error("Failed to find notfound lyrics: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findAll completed for notfound lyrics")
    }.flowOn(Dispatchers.IO)

    override fun findAllOrderedByDate(): Flow<Resource<List<NotFoundLyric>>> = flow {
        logger.debug("findAllOrderedByDate started for notfound lyrics")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.listDocuments for all notfound lyrics ordered by date")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId,
                queries = listOf(Query.orderDesc("addedDate"))
            )
        }.fold(
            onSuccess = { documents ->
                logger.debug("Successfully found ${documents.documents.size} notfound lyrics ordered by date")
                emit(Resource.Success(documents.documents.map { documentToNotFoundLyric(it) }))
            },
            onFailure = { e ->
                logger.error("Error finding all notfound lyrics ordered by date", e)
                emit(Resource.Error("Failed to find notfound lyrics: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findAllOrderedByDate completed for notfound lyrics")
    }.flowOn(Dispatchers.IO)

    override fun save(notFoundLyric: NotFoundLyric): Flow<Resource<NotFoundLyric>> = flow {
        logger.debug("save started for notfound lyric id: ${notFoundLyric.videoId}")
        emit(Resource.Loading)
        
        val data = mapOf(
            "videoId" to notFoundLyric.videoId,
            "addedDate" to notFoundLyric.addedDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        
        runCatching {
            logger.debug("Calling databases.createDocument for notfound lyric")
            databases.createDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = ID.unique(),
                data = data
            )
        }.fold(
            onSuccess = { document ->
                logger.debug("Successfully saved notfound lyric with id: ${notFoundLyric.videoId}")
                emit(Resource.Success(documentToNotFoundLyric(document)))
            },
            onFailure = { e ->
                logger.error("Error saving notfound lyric with id: ${notFoundLyric.videoId}", e)
                emit(Resource.Error("Failed to save notfound lyric: ${e.message}", e as? Exception))
            }
        )
        logger.debug("save completed for notfound lyric id: ${notFoundLyric.videoId}")
    }.flowOn(Dispatchers.IO)

    override fun delete(id: String): Flow<Resource<Boolean>> = flow {
        logger.debug("delete started for notfound lyric id: $id")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.deleteDocument for notfound lyric id: $id")
            databases.deleteDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = id
            )
        }.fold(
            onSuccess = {
                logger.debug("Successfully deleted notfound lyric with id: $id")
                emit(Resource.Success(true))
            },
            onFailure = { e ->
                logger.error("Error deleting notfound lyric with id: $id", e)
                if (e is AppwriteException && e.code == 404) {
                    emit(Resource.Success(false))
                } else {
                    emit(Resource.Error("Failed to delete notfound lyric: ${e.message}", e as? Exception))
                }
            }
        )
        logger.debug("delete completed for notfound lyric id: $id")
    }.flowOn(Dispatchers.IO)

    override fun deleteByVideoId(videoId: String): Flow<Resource<Boolean>> = flow {
        logger.debug("deleteByVideoId started for videoId: $videoId")
        emit(Resource.Loading)
        
        runCatching {
            // First find the document to get its ID
            logger.debug("Finding notfound lyric document for videoId: $videoId")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId,
                queries = listOf(Query.equal("videoId", videoId))
            )
        }.fold(
            onSuccess = { documents ->
                if (documents.documents.isNotEmpty()) {
                    val documentId = documents.documents.first().id
                    runCatching {
                        logger.debug("Deleting notfound lyric document with documentId: $documentId for videoId: $videoId")
                        databases.deleteDocument(
                            databaseId = databaseId,
                            collectionId = collectionId,
                            documentId = documentId
                        )
                    }.fold(
                        onSuccess = {
                            logger.debug("Successfully deleted notfound lyric for videoId: $videoId")
                            emit(Resource.Success(true))
                        },
                        onFailure = { e ->
                            logger.error("Error deleting notfound lyric for videoId: $videoId", e)
                            emit(Resource.Error("Failed to delete notfound lyric: ${e.message}", e as? Exception))
                        }
                    )
                } else {
                    logger.debug("No notfound lyric found for videoId: $videoId")
                    emit(Resource.Success(false))
                }
            },
            onFailure = { e ->
                logger.error("Error finding notfound lyric for videoId: $videoId", e)
                emit(Resource.Error("Failed to find notfound lyric for deletion: ${e.message}", e as? Exception))
            }
        )
        logger.debug("deleteByVideoId completed for videoId: $videoId")
    }.flowOn(Dispatchers.IO)

    private fun documentToNotFoundLyric(document: Document<Map<String, Any>>): NotFoundLyric {
        return NotFoundLyric(
            videoId = document.data["videoId"] as String,
            addedDate = LocalDateTime.parse(document.data["addedDate"] as String, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        )
    }
} 