package org.simpmusic.lyrics.infrastructure.persistence

import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.Document
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.simpmusic.lyrics.domain.model.Lyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.extensions.sha256
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

/**
 * Appwrite implementation of the LyricRepository
 */
@Repository("appwriteLyricRepositoryImpl")
class AppwriteLyricRepository(
    private val databases: Databases,
    @Qualifier("databaseId") private val databaseId: String,
    @Qualifier("lyricsCollectionId") private val collectionId: String,
) : LyricRepository {
    private val logger = LoggerFactory.getLogger(AppwriteLyricRepository::class.java)

    override fun findById(id: String): Flow<Resource<Lyric?>> =
        flow {
            logger.debug("findById started for id: $id")
            runCatching {
                logger.debug("Calling databases.getDocument for id: $id")
                databases.getDocument(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    documentId = id,
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
                },
            )
            logger.debug("findById completed for id: $id")
        }.flowOn(Dispatchers.IO)

    override fun findBySongTitle(
        title: String,
        limit: Int?,
        offset: Int?,
    ): Flow<Resource<List<Lyric>>> =
        flow {
            logger.debug("findBySongTitle started for title: $title, limit: $limit, offset: $offset")
            val queries = mutableListOf(Query.contains("songTitle", title))
            limit?.let { queries.add(Query.limit(it)) }
            offset?.let { queries.add(Query.offset(it)) }

            runCatching {
                logger.debug("Calling databases.listDocuments with title query: $title")
                databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries = queries,
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
                },
            )
            logger.debug("findBySongTitle completed for title: $title")
        }.flowOn(Dispatchers.IO)

    override fun findByArtist(
        artist: String,
        limit: Int?,
        offset: Int?,
    ): Flow<Resource<List<Lyric>>> =
        flow {
            logger.debug("findByArtist started for artist: $artist, limit: $limit, offset: $offset")
            val queries = mutableListOf(Query.contains("artistName", artist))
            limit?.let { queries.add(Query.limit(it)) }
            offset?.let { queries.add(Query.offset(it)) }

            runCatching {
                logger.debug("Calling databases.listDocuments with artist query: $artist")
                databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries = queries,
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
                },
            )
            logger.debug("findByArtist completed for artist: $artist")
        }.flowOn(Dispatchers.IO)

    override fun findByVideoId(
        videoId: String,
        limit: Int?,
        offset: Int?,
    ): Flow<Resource<List<Lyric>>> =
        flow {
            logger.debug("findByVideoId started for videoId: $videoId, limit: $limit, offset: $offset")
            val queries = mutableListOf(Query.equal("videoId", videoId))
            limit?.let { queries.add(Query.limit(it)) }
            offset?.let { queries.add(Query.offset(it)) }

            runCatching {
                logger.debug("Calling databases.listDocuments with videoId query: $videoId")
                databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries = queries,
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
                },
            )
            logger.debug("findByVideoId completed for videoId: $videoId")
        }.flowOn(Dispatchers.IO)

    override fun search(
        keywords: String,
        limit: Int?,
        offset: Int?,
    ): Flow<Resource<List<Lyric>>> =
        flow {
            logger.debug("search started for keywords: $keywords, limit: $limit, offset: $offset")
            val queries =
                mutableListOf(
                    Query.or(
                        listOf(
                            Query.search("songTitle", keywords),
                            Query.search("artistName", keywords),
                        ),
                    ),
                )

            limit?.let { queries.add(Query.limit(it)) }
            offset?.let { queries.add(Query.offset(it)) }

            runCatching {
                logger.debug("Calling databases.listDocuments with full-text search query: $keywords")
                databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries = queries,
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
                },
            )
            logger.debug("search completed for keywords: $keywords")
        }.flowOn(Dispatchers.IO)

    override fun findBySha256Hash(sha256hash: String): Flow<Resource<Lyric?>> =
        flow {
            logger.debug("findBySha256Hash --> Started for hash: $sha256hash")
            runCatching {
                logger.debug("findBySha256Hash --> Calling databases.listDocuments with sha256hash query: $sha256hash")
                databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries = listOf(Query.equal("sha256hash", sha256hash)),
                )
            }.fold(
                onSuccess = { documents ->
                    val lyric = documents.documents.firstOrNull()?.let { documentToLyric(it) }
                    logger.debug("findBySha256Hash --> Found lyric with sha256hash: $sha256hash: ${lyric != null}")
                    emit(Resource.Success(lyric))
                },
                onFailure = { e ->
                    e.printStackTrace()
                    logger.error("findBySha256Hash --> Error finding lyric by sha256hash: $sha256hash", e)
                    emit(Resource.Error("Failed to find lyric by sha256hash: ${e.message}", e as? Exception))
                },
            )
            logger.debug("findBySha256Hash --> Completed for hash: $sha256hash")
        }.flowOn(Dispatchers.IO)

    override fun save(lyric: Lyric): Flow<Resource<Lyric>> =
        flow {
            logger.debug("save started for lyric id: ${lyric.id}")
            val data =
                mapOf(
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
                    "contributorEmail" to lyric.contributorEmail,
                    "sha256hash" to lyric.sha256hash,
                )
            logger.debug("Prepared data for save: {}", data.keys)

            runCatching {
                logger.debug("Checking if document exists for id: ${lyric.id}")

                // Try to get existing document first - simple approach
                var documentExists: Boolean
                try {
                    databases.getDocument(
                        databaseId = databaseId,
                        collectionId = collectionId,
                        documentId = lyric.id,
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
                        documentId = lyric.id,
                        data = data,
                    )
                } else {
                    logger.debug("Creating new document for id: ${lyric.id}")
                    databases.createDocument(
                        databaseId = databaseId,
                        collectionId = collectionId,
                        documentId = lyric.id,
                        data = data,
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
                },
            )
            logger.debug("save completed for lyric id: ${lyric.id}")
        }.flowOn(Dispatchers.IO)

    override fun updateVote(
        id: String,
        voteIncrement: Int,
    ): Flow<Resource<Lyric>> =
        flow {
            logger.debug("updateVote --> Started for id: $id with increment: $voteIncrement")
            runCatching {
                // First get the lyric to update its vote count
                logger.debug("updateVote --> Getting current lyric for id: $id")
                val document =
                    databases.getDocument(
                        databaseId = databaseId,
                        collectionId = collectionId,
                        documentId = id,
                    )

                val lyric = documentToLyric(document)
                val newVoteCount = lyric.vote + voteIncrement

                logger.debug("updateVote --> Updating vote count from ${lyric.vote} to $newVoteCount for id: $id")

                // Update only the vote field
                val updatedDocument =
                    databases.updateDocument(
                        databaseId = databaseId,
                        collectionId = collectionId,
                        documentId = id,
                        data = mapOf("vote" to newVoteCount),
                    )

                val updatedLyric = documentToLyric(updatedDocument)
                emit(Resource.Success(updatedLyric))
            }.fold(
                onSuccess = { lyric ->
                    logger.debug("updateVote --> Successfully updated vote for id: $id")
                },
                onFailure = { e ->
                    logger.error("updateVote --> Error updating vote for id: $id", e)
                    if (e is AppwriteException && e.code == 404) {
                        emit(Resource.notFoundError("Lyric not found with id: $id"))
                    } else {
                        emit(Resource.Error("Failed to update vote: ${e.message}", e as? Exception))
                    }
                },
            )
            logger.debug("updateVote --> Completed for id: $id")
        }.flowOn(Dispatchers.IO)

    private fun documentToLyric(document: Document<Map<String, Any>>): Lyric {
        logger.debug("Converting document to Lyric: ${document.id}")
        logger.debug("Document data keys: {}", document.data.keys)
        logger.debug("Document data: {}.data", document)

        runCatching {
            val videoId = document.data["videoId"].toString()
            val durationSeconds = document.data["durationSeconds"].toString().toInt()
            val plainLyric = document.data["plainLyric"].toString()
            val syncedLyrics = document.data["syncedLyrics"]?.toString()
            val richSyncLyrics = document.data["richSyncLyrics"]?.toString()

            val lyric =
                Lyric(
                    id = document.data["id"].toString(),
                    videoId = videoId,
                    songTitle = document.data["songTitle"].toString(),
                    artistName = document.data["artistName"].toString(),
                    albumName = document.data["albumName"].toString(),
                    durationSeconds = durationSeconds,
                    plainLyric = plainLyric,
                    syncedLyrics = syncedLyrics,
                    richSyncLyrics = richSyncLyrics,
                    vote = document.data["vote"].toString().toInt(),
                    contributor = document.data["contributor"].toString(),
                    contributorEmail = document.data["contributorEmail"].toString(),
                    sha256hash =
                        document.data["sha256hash"]?.toString()
                            ?: "$videoId-$durationSeconds-$plainLyric-$syncedLyrics-$richSyncLyrics".sha256(),
                )
            logger.debug("Successfully converted document to Lyric: ${lyric.id}")
            return lyric
        }.getOrElse { e ->
            logger.error("Error converting document to Lyric", e)
            throw e
        }
    }
}
