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
import org.simpmusic.lyrics.domain.model.TranslatedLyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Appwrite implementation of the TranslatedLyricRepository
 */
@Repository("appwriteTranslatedLyricRepositoryImpl")
@OptIn(ExperimentalUuidApi::class)
class AppwriteTranslatedLyricRepository(
    private val databases: Databases,
    @Qualifier("databaseId") private val databaseId: String,
    @Qualifier("translatedLyricsCollectionId") private val collectionId: String
) : TranslatedLyricRepository {
    
    private val logger = LoggerFactory.getLogger(AppwriteTranslatedLyricRepository::class.java)

    override fun findById(id: String): Flow<Resource<TranslatedLyric?>> = flow {
        logger.debug("findById started for id: $id")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.getDocument for translated lyric id: $id")
            databases.getDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = id
            )
        }.fold(
            onSuccess = { document ->
                logger.debug("Successfully found translated lyric document for id: $id")
                emit(Resource.Success(documentToTranslatedLyric(document)))
            },
            onFailure = { e ->
                logger.debug("Failed to find translated lyric document for id: $id, error: ${e.message}")
                if (e is AppwriteException && e.code == 404) {
                    logger.debug("Translated lyric document not found (404) for id: $id")
                    emit(Resource.Success<TranslatedLyric?>(null))
                } else {
                    logger.error("Error finding translated lyric by id: $id", e)
                    emit(Resource.Error("Failed to find translated lyric: ${e.message}", e as? Exception))
                }
            }
        )
        logger.debug("findById completed for translated lyric id: $id")
    }.flowOn(Dispatchers.IO)

    override fun findByVideoId(videoId: String): Flow<Resource<List<TranslatedLyric>>> = flow {
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
                logger.debug("Successfully found ${documents.documents.size} translated lyrics for videoId: $videoId")
                emit(Resource.Success(documents.documents.map { documentToTranslatedLyric(it) }))
            },
            onFailure = { e ->
                logger.error("Error finding translated lyrics by videoId: $videoId", e)
                emit(Resource.Error("Failed to find translated lyrics by videoId: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findByVideoId completed for videoId: $videoId")
    }.flowOn(Dispatchers.IO)

    override fun findByVideoIdAndLanguage(videoId: String, language: String): Flow<Resource<TranslatedLyric?>> = flow {
        logger.debug("findByVideoIdAndLanguage started for videoId: $videoId, language: $language")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.listDocuments with videoId and language query")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId,
                queries = listOf(
                    Query.equal("videoId", videoId),
                    Query.equal("language", language)
                )
            )
        }.fold(
            onSuccess = { documents ->
                val translatedLyric = documents.documents.firstOrNull()?.let { documentToTranslatedLyric(it) }
                logger.debug("Found translated lyric for videoId: $videoId, language: $language: ${translatedLyric != null}")
                emit(Resource.Success(translatedLyric))
            },
            onFailure = { e ->
                logger.error("Error finding translated lyric by videoId and language: $videoId, $language", e)
                emit(Resource.Error("Failed to find translated lyric: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findByVideoIdAndLanguage completed for videoId: $videoId, language: $language")
    }.flowOn(Dispatchers.IO)

    override fun findByLanguage(language: String): Flow<Resource<List<TranslatedLyric>>> = flow {
        logger.debug("findByLanguage started for language: $language")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.listDocuments with language query: $language")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId,
                queries = listOf(Query.equal("language", language))
            )
        }.fold(
            onSuccess = { documents ->
                logger.debug("Successfully found ${documents.documents.size} translated lyrics for language: $language")
                emit(Resource.Success(documents.documents.map { documentToTranslatedLyric(it) }))
            },
            onFailure = { e ->
                logger.error("Error finding translated lyrics by language: $language", e)
                emit(Resource.Error("Failed to find translated lyrics by language: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findByLanguage completed for language: $language")
    }.flowOn(Dispatchers.IO)

    override fun findAll(): Flow<Resource<List<TranslatedLyric>>> = flow {
        logger.debug("findAll started for translated lyrics")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.listDocuments for all translated lyrics")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId
            )
        }.fold(
            onSuccess = { documents ->
                logger.debug("Successfully found ${documents.documents.size} translated lyrics")
                emit(Resource.Success(documents.documents.map { documentToTranslatedLyric(it) }))
            },
            onFailure = { e ->
                logger.error("Error finding all translated lyrics", e)
                emit(Resource.Error("Failed to find translated lyrics: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findAll completed for translated lyrics")
    }.flowOn(Dispatchers.IO)

    override fun save(translatedLyric: TranslatedLyric): Flow<Resource<TranslatedLyric>> = flow {
        logger.debug("save started for translated lyric id: ${translatedLyric.id}")
        emit(Resource.Loading)
        
        val data = mapOf(
            "id" to translatedLyric.id.toString(),
            "videoId" to translatedLyric.videoId,
            "translatedLyric" to translatedLyric.translatedLyric,
            "language" to translatedLyric.language,
            "vote" to translatedLyric.vote,
            "contributor" to translatedLyric.contributor,
            "contributorEmail" to translatedLyric.contributorEmail
        )
        
        runCatching {
            logger.debug("Calling databases.createDocument for translated lyric")
            databases.createDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = ID.unique(),
                data = data
            )
        }.fold(
            onSuccess = { document ->
                logger.debug("Successfully saved translated lyric with id: ${translatedLyric.id}")
                emit(Resource.Success(documentToTranslatedLyric(document)))
            },
            onFailure = { e ->
                logger.error("Error saving translated lyric with id: ${translatedLyric.id}", e)
                emit(Resource.Error("Failed to save translated lyric: ${e.message}", e as? Exception))
            }
        )
        logger.debug("save completed for translated lyric id: ${translatedLyric.id}")
    }.flowOn(Dispatchers.IO)

    override fun delete(id: String): Flow<Resource<Boolean>> = flow {
        logger.debug("delete started for translated lyric id: $id")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("Calling databases.deleteDocument for translated lyric id: $id")
            databases.deleteDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = id
            )
        }.fold(
            onSuccess = {
                logger.debug("Successfully deleted translated lyric with id: $id")
                emit(Resource.Success(true))
            },
            onFailure = { e ->
                logger.error("Error deleting translated lyric with id: $id", e)
                if (e is AppwriteException && e.code == 404) {
                    emit(Resource.Success(false))
                } else {
                    emit(Resource.Error("Failed to delete translated lyric: ${e.message}", e as? Exception))
                }
            }
        )
        logger.debug("delete completed for translated lyric id: $id")
    }.flowOn(Dispatchers.IO)

    private fun documentToTranslatedLyric(document: Document<Map<String, Any>>): TranslatedLyric {
        return TranslatedLyric(
            id = Uuid.parse(document.data["id"] as String),
            videoId = document.data["videoId"] as String,
            translatedLyric = document.data["translatedLyric"] as String,
            language = document.data["language"] as String,
            vote = (document.data["vote"] as Number).toInt(),
            contributor = document.data["contributor"] as String,
            contributorEmail = document.data["contributorEmail"] as String
        )
    }
} 