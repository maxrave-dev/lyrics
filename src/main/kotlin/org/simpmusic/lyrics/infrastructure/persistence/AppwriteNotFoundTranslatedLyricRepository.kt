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
import org.simpmusic.lyrics.domain.model.NotFoundTranslatedLyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.NotFoundTranslatedLyricRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Appwrite implementation of the NotFoundTranslatedLyricRepository
 */
@Repository("appwriteNotFoundTranslatedLyricRepositoryImpl")
class AppwriteNotFoundTranslatedLyricRepository(
    private val databases: Databases,
    @Qualifier("databaseId") private val databaseId: String,
    @Qualifier("notFoundTranslatedLyricsCollectionId") private val collectionId: String,
) : NotFoundTranslatedLyricRepository {
    private val logger = LoggerFactory.getLogger(AppwriteNotFoundTranslatedLyricRepository::class.java)

    override fun findById(id: String): Flow<Resource<NotFoundTranslatedLyric?>> =
        flow {
            logger.debug("findById --> Finding notfound translated lyric for id: $id")
            runCatching {
                logger.debug("findById --> Calling databases.getDocument for notfound translated lyric id: $id")
                databases.getDocument(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    documentId = id,
                )
            }.fold(
                onSuccess = { document ->
                    logger.debug("findById --> Successfully found notfound translated lyric document for id: $id")
                    emit(Resource.Success(documentToNotFoundTranslatedLyric(document)))
                },
                onFailure = { e ->
                    logger.debug("findById --> Failed to find notfound translated lyric document for id: $id, error: ${e.message}")
                    if (e is AppwriteException && e.code == 404) {
                        logger.debug("findById --> NotFound translated lyric document not found (404) for id: $id")
                        emit(Resource.Success<NotFoundTranslatedLyric?>(null))
                    } else {
                        logger.error("findById --> Error finding notfound translated lyric by id: $id", e)
                        emit(Resource.Error("Failed to find notfound translated lyric: ${e.message}", e as? Exception))
                    }
                },
            )
            logger.debug("findById --> findById completed for notfound translated lyric id: $id")
        }.flowOn(Dispatchers.IO)

    override fun findByVideoIdAndLanguage(
        videoId: String,
        language: String,
    ): Flow<Resource<NotFoundTranslatedLyric?>> =
        flow {
            logger.debug("findByVideoIdAndLanguage --> Finding notfound translated lyric for videoId: $videoId, language: $language")
            runCatching {
                logger.debug("findByVideoIdAndLanguage --> Calling databases.listDocuments with videoId and language query")
                databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries =
                        listOf(
                            Query.equal("videoId", videoId),
                            Query.equal("translationLanguage", language),
                        ),
                )
            }.fold(
                onSuccess = { documents ->
                    val notFoundTranslatedLyric =
                        documents.documents.firstOrNull()?.let { documentToNotFoundTranslatedLyric(it) }
                    logger.debug(
                        "findByVideoIdAndLanguage --> Found notfound translated lyric for videoId: $videoId, language: $language: ${notFoundTranslatedLyric != null}",
                    )
                    emit(Resource.Success(notFoundTranslatedLyric))
                },
                onFailure = { e ->
                    logger.error(
                        "findByVideoIdAndLanguage --> Error finding notfound translated lyric by videoId: $videoId, language: $language",
                        e,
                    )
                    emit(
                        Resource.Error(
                            "Failed to find notfound translated lyric by videoId and language: ${e.message}",
                            e as? Exception,
                        ),
                    )
                },
            )
            logger.debug("findByVideoIdAndLanguage --> findByVideoIdAndLanguage completed for videoId: $videoId, language: $language")
        }.flowOn(Dispatchers.IO)

    override fun findBySha256Hash(sha256hash: String): Flow<Resource<NotFoundTranslatedLyric?>> =
        flow {
            logger.debug("findBySha256Hash --> Finding notfound translated lyric for sha256hash: $sha256hash")
            runCatching {
                logger.debug("findBySha256Hash --> Calling databases.listDocuments with sha256hash query")
                databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries = listOf(Query.equal("sha256hash", sha256hash)),
                )
            }.fold(
                onSuccess = { documents ->
                    val notFoundTranslatedLyric =
                        documents.documents.firstOrNull()?.let { documentToNotFoundTranslatedLyric(it) }
                    logger.debug(
                        "findBySha256Hash --> Found notfound translated lyric for sha256hash: $sha256hash: ${notFoundTranslatedLyric != null}",
                    )
                    emit(Resource.Success(notFoundTranslatedLyric))
                },
                onFailure = { e ->
                    logger.error(
                        "findBySha256Hash --> Error finding notfound translated lyric by sha256hash: $sha256hash",
                        e,
                    )
                    emit(
                        Resource.Error(
                            "Failed to find notfound translated lyric by sha256hash: ${e.message}",
                            e as? Exception,
                        ),
                    )
                },
            )
            logger.debug("findBySha256Hash --> findBySha256Hash completed for sha256hash: $sha256hash")
        }.flowOn(Dispatchers.IO)

    override fun findAllOrderedByDate(
        limit: Int?,
        offset: Int?,
    ): Flow<Resource<List<NotFoundTranslatedLyric>>> =
        flow {
            logger.debug("findAllOrderedByDate --> Finding all notfound translated lyrics, limit: $limit, offset: $offset")
            val queries = mutableListOf(Query.orderDesc("addedDate"))
            limit?.let { queries.add(Query.limit(it)) }
            offset?.let { queries.add(Query.offset(it)) }

            runCatching {
                logger.debug("findAllOrderedByDate --> Calling databases.listDocuments for all notfound translated lyrics ordered by date")
                databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries = queries,
                )
            }.fold(
                onSuccess = { documents ->
                    logger.debug(
                        "findAllOrderedByDate --> Successfully found ${documents.documents.size} notfound translated lyrics ordered by date",
                    )
                    emit(Resource.Success(documents.documents.map { documentToNotFoundTranslatedLyric(it) }))
                },
                onFailure = { e ->
                    logger.error("findAllOrderedByDate --> Error finding all notfound translated lyrics ordered by date", e)
                    emit(Resource.Error("Failed to find notfound translated lyrics: ${e.message}", e as? Exception))
                },
            )
            logger.debug("findAllOrderedByDate --> findAllOrderedByDate completed for notfound translated lyrics")
        }.flowOn(Dispatchers.IO)

    override fun save(notFoundTranslatedLyric: NotFoundTranslatedLyric): Flow<Resource<NotFoundTranslatedLyric>> =
        flow {
            logger.debug("save --> Saving notfound translated lyric with sha256hash: ${notFoundTranslatedLyric.sha256hash}")
            val data =
                mapOf(
                    "videoId" to notFoundTranslatedLyric.videoId,
                    "translationLanguage" to notFoundTranslatedLyric.translationLanguage,
                    "addedDate" to notFoundTranslatedLyric.addedDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "sha256hash" to notFoundTranslatedLyric.sha256hash,
                )

            runCatching {
                logger.debug("save --> Calling databases.createDocument for notfound translated lyric")
                databases.createDocument(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    documentId = ID.unique(),
                    data = data,
                )
            }.fold(
                onSuccess = { document ->
                    logger.debug(
                        "save --> Successfully saved notfound translated lyric with sha256hash: ${notFoundTranslatedLyric.sha256hash}",
                    )
                    emit(Resource.Success(documentToNotFoundTranslatedLyric(document)))
                },
                onFailure = { e ->
                    logger.error(
                        "save --> Error saving notfound translated lyric with sha256hash: ${notFoundTranslatedLyric.sha256hash}",
                        e,
                    )
                    emit(Resource.Error("Failed to save notfound translated lyric: ${e.message}", e as? Exception))
                },
            )
            logger.debug("save --> save completed for notfound translated lyric with sha256hash: ${notFoundTranslatedLyric.sha256hash}")
        }.flowOn(Dispatchers.IO)

    override fun deleteByVideoIdAndLanguage(
        videoId: String,
        language: String,
    ): Flow<Resource<Boolean>> =
        flow {
            logger.debug("deleteByVideoIdAndLanguage --> Deleting notfound translated lyric for videoId: $videoId, language: $language")
            runCatching {
                // First find the document to get its ID
                logger.debug(
                    "deleteByVideoIdAndLanguage --> Finding notfound translated lyric document for videoId: $videoId, language: $language",
                )
                databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries =
                        listOf(
                            Query.equal("videoId", videoId),
                            Query.equal("translationLanguage", language),
                        ),
                )
            }.fold(
                onSuccess = { documents ->
                    if (documents.documents.isNotEmpty()) {
                        val documentId = documents.documents.first().id
                        runCatching {
                            logger.debug(
                                "deleteByVideoIdAndLanguage --> Deleting notfound translated lyric document with documentId: $documentId",
                            )
                            databases.deleteDocument(
                                databaseId = databaseId,
                                collectionId = collectionId,
                                documentId = documentId,
                            )
                        }.fold(
                            onSuccess = {
                                logger.debug(
                                    "deleteByVideoIdAndLanguage --> Successfully deleted notfound translated lyric for videoId: $videoId, language: $language",
                                )
                                emit(Resource.Success(true))
                            },
                            onFailure = { e ->
                                logger.error(
                                    "deleteByVideoIdAndLanguage --> Error deleting notfound translated lyric for videoId: $videoId, language: $language",
                                    e,
                                )
                                emit(
                                    Resource.Error(
                                        "Failed to delete notfound translated lyric: ${e.message}",
                                        e as? Exception,
                                    ),
                                )
                            },
                        )
                    } else {
                        logger.debug(
                            "deleteByVideoIdAndLanguage --> No notfound translated lyric found for videoId: $videoId, language: $language",
                        )
                        emit(Resource.Success(false))
                    }
                },
                onFailure = { e ->
                    logger.error(
                        "deleteByVideoIdAndLanguage --> Error finding notfound translated lyric for videoId: $videoId, language: $language",
                        e,
                    )
                    emit(
                        Resource.Error(
                            "Failed to find notfound translated lyric for deletion: ${e.message}",
                            e as? Exception,
                        ),
                    )
                },
            )
            logger.debug("deleteByVideoIdAndLanguage --> deleteByVideoIdAndLanguage completed for videoId: $videoId, language: $language")
        }.flowOn(Dispatchers.IO)

    private fun documentToNotFoundTranslatedLyric(document: Document<Map<String, Any>>): NotFoundTranslatedLyric =
        NotFoundTranslatedLyric(
            videoId = document.data["videoId"] as String,
            translationLanguage = document.data["translationLanguage"] as String,
            addedDate =
                LocalDateTime.parse(
                    document.data["addedDate"] as String,
                    DateTimeFormatter.ISO_ZONED_DATE_TIME,
                ),
            sha256hash = document.data["sha256hash"] as String,
        )
}
