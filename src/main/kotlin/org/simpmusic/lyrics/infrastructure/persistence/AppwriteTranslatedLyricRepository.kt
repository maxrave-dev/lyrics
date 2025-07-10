package org.simpmusic.lyrics.infrastructure.persistence

import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.simpmusic.lyrics.domain.model.TranslatedLyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.simpmusic.lyrics.extensions.documentToTranslatedLyric
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

/**
 * Appwrite implementation of the TranslatedLyricRepository
 */
@Repository("appwriteTranslatedLyricRepositoryImpl")
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

    override fun findBySha256Hash(sha256hash: String): Flow<Resource<TranslatedLyric?>> = flow {
        logger.debug("findBySha256Hash --> Started for hash: $sha256hash")
        emit(Resource.Loading)
        
        runCatching {
            logger.debug("findBySha256Hash --> Calling databases.listDocuments with sha256hash query: $sha256hash")
            databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId,
                queries = listOf(Query.equal("sha256hash", sha256hash))
            )
        }.fold(
            onSuccess = { documents ->
                val translatedLyric = documents.documents.firstOrNull()?.let { documentToTranslatedLyric(it) }
                logger.debug("findBySha256Hash --> Found translated lyric with sha256hash: $sha256hash: ${translatedLyric != null}")
                emit(Resource.Success(translatedLyric))
            },
            onFailure = { e ->
                logger.error("findBySha256Hash --> Error finding translated lyric by sha256hash: $sha256hash", e)
                emit(Resource.Error("Failed to find translated lyric by sha256hash: ${e.message}", e as? Exception))
            }
        )
        logger.debug("findBySha256Hash --> Completed for hash: $sha256hash")
    }.flowOn(Dispatchers.IO)

    override fun save(translatedLyric: TranslatedLyric): Flow<Resource<TranslatedLyric>> = flow {
        logger.debug("save started for translated lyric id: ${translatedLyric.id}")
        emit(Resource.Loading)
        
        val data = mapOf(
            "id" to translatedLyric.id,
            "videoId" to translatedLyric.videoId,
            "translatedLyric" to translatedLyric.translatedLyric,
            "language" to translatedLyric.language,
            "vote" to translatedLyric.vote,
            "contributor" to translatedLyric.contributor,
            "contributorEmail" to translatedLyric.contributorEmail,
            "sha256hash" to translatedLyric.sha256hash
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

    override fun updateVote(id: String, voteIncrement: Int): Flow<Resource<TranslatedLyric>> = flow {
        logger.debug("updateVote --> Started for translated lyric id: $id with increment: $voteIncrement")
        emit(Resource.Loading)
        
        runCatching {
            // First get the translated lyric to update its vote count
            logger.debug("updateVote --> Getting current translated lyric for id: $id")
            val document = databases.getDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = id
            )
            
            val translatedLyric = documentToTranslatedLyric(document)
            val newVoteCount = translatedLyric.vote + voteIncrement
            
            logger.debug("updateVote --> Updating vote count from ${translatedLyric.vote} to $newVoteCount for id: $id")
            
            // Update only the vote field
            val updatedDocument = databases.updateDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = id,
                data = mapOf("vote" to newVoteCount)
            )
            
            val updatedTranslatedLyric = documentToTranslatedLyric(updatedDocument)
            emit(Resource.Success(updatedTranslatedLyric))
        }.fold(
            onSuccess = { translatedLyric ->
                logger.debug("updateVote --> Successfully updated vote for translated lyric id: $id")
            },
            onFailure = { e ->
                logger.error("updateVote --> Error updating vote for translated lyric id: $id", e)
                if (e is AppwriteException && e.code == 404) {
                    emit(Resource.notFoundError("Translated lyric not found with id: $id"))
                } else {
                    emit(Resource.Error("Failed to update vote: ${e.message}", e as? Exception))
                }
            }
        )
        logger.debug("updateVote --> Completed for translated lyric id: $id")
    }.flowOn(Dispatchers.IO)
}