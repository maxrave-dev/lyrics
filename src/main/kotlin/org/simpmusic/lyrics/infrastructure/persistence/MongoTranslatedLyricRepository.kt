package org.simpmusic.lyrics.infrastructure.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.model.TranslatedLyric
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

/**
 * MongoDB implementation of the TranslatedLyricRepository
 */
@Repository("mongoTranslatedLyricRepositoryImpl")
class MongoTranslatedLyricRepository(
    private val mongoTemplate: MongoTemplate,
) : TranslatedLyricRepository {
    private val logger = LoggerFactory.getLogger(MongoTranslatedLyricRepository::class.java)

    companion object {
        private const val COLLECTION_NAME = "translated_lyrics"
    }

    override fun findById(id: String): Flow<Resource<TranslatedLyric?>> =
        flow {
            logger.debug("findById started for id: $id")
            runCatching {
                logger.debug("Calling mongoTemplate.findById for translated lyric id: $id")
                mongoTemplate.findById(id, TranslatedLyric::class.java, COLLECTION_NAME)
            }.fold(
                onSuccess = { translatedLyric ->
                    logger.debug("Successfully found translated lyric document for id: $id")
                    emit(Resource.Success(translatedLyric))
                },
                onFailure = { e ->
                    logger.debug("Failed to find translated lyric document for id: $id, error: ${e.message}")
                    logger.error("Error finding translated lyric by id: $id", e)
                    emit(Resource.Error("Failed to find translated lyric: ${e.message}", e as? Exception))
                },
            )
            logger.debug("findById completed for translated lyric id: $id")
        }.flowOn(Dispatchers.IO)

    override fun findByVideoId(
        videoId: String,
        limit: Int?,
        offset: Int?,
    ): Flow<Resource<List<TranslatedLyric>>> =
        flow {
            logger.debug("findByVideoId started for videoId: $videoId, limit: $limit, offset: $offset")
            runCatching {
                val query = Query(Criteria.where("videoId").`is`(videoId))

                offset?.let { query.skip(it.toLong()) }
                limit?.let { query.limit(it) }

                logger.debug("Calling mongoTemplate.find with videoId query: $videoId")
                mongoTemplate.find(query, TranslatedLyric::class.java, COLLECTION_NAME)
            }.fold(
                onSuccess = { translatedLyrics ->
                    logger.debug("Successfully found ${translatedLyrics.size} translated lyrics for videoId: $videoId")
                    emit(Resource.Success(translatedLyrics))
                },
                onFailure = { e ->
                    logger.error("Error finding translated lyrics by videoId: $videoId", e)
                    emit(Resource.Error("Failed to find translated lyrics by videoId: ${e.message}", e as? Exception))
                },
            )
            logger.debug("findByVideoId completed for videoId: $videoId")
        }.flowOn(Dispatchers.IO)

    override fun findByVideoIdAndLanguage(
        videoId: String,
        language: String,
    ): Flow<Resource<TranslatedLyric?>> =
        flow {
            logger.debug("findByVideoIdAndLanguage started for videoId: $videoId, language: $language")
            runCatching {
                val query =
                    Query(
                        Criteria
                            .where("videoId")
                            .`is`(videoId)
                            .and("language")
                            .`is`(language),
                    )

                logger.debug("Calling mongoTemplate.findOne with videoId and language query")
                mongoTemplate.findOne(query, TranslatedLyric::class.java, COLLECTION_NAME)
            }.fold(
                onSuccess = { translatedLyric ->
                    logger.debug("Found translated lyric for videoId: $videoId, language: $language: ${translatedLyric != null}")
                    emit(Resource.Success(translatedLyric))
                },
                onFailure = { e ->
                    logger.error("Error finding translated lyric by videoId and language: $videoId, $language", e)
                    emit(Resource.Error("Failed to find translated lyric: ${e.message}", e as? Exception))
                },
            )
            logger.debug("findByVideoIdAndLanguage completed for videoId: $videoId, language: $language")
        }.flowOn(Dispatchers.IO)

    override fun findByLanguage(
        language: String,
        limit: Int?,
        offset: Int?,
    ): Flow<Resource<List<TranslatedLyric>>> =
        flow {
            logger.debug("findByLanguage started for language: $language, limit: $limit, offset: $offset")
            runCatching {
                val query = Query(Criteria.where("language").`is`(language))

                offset?.let { query.skip(it.toLong()) }
                limit?.let { query.limit(it) }

                logger.debug("Calling mongoTemplate.find with language query: $language")
                mongoTemplate.find(query, TranslatedLyric::class.java, COLLECTION_NAME)
            }.fold(
                onSuccess = { translatedLyrics ->
                    logger.debug("Successfully found ${translatedLyrics.size} translated lyrics for language: $language")
                    emit(Resource.Success(translatedLyrics))
                },
                onFailure = { e ->
                    logger.error("Error finding translated lyrics by language: $language", e)
                    emit(Resource.Error("Failed to find translated lyrics by language: ${e.message}", e as? Exception))
                },
            )
            logger.debug("findByLanguage completed for language: $language")
        }.flowOn(Dispatchers.IO)

    override fun findBySha256Hash(sha256hash: String): Flow<Resource<TranslatedLyric?>> =
        flow {
            logger.debug("findBySha256Hash --> Started for hash: $sha256hash")
            runCatching {
                logger.debug("findBySha256Hash --> Calling mongoTemplate.findOne with sha256hash query: $sha256hash")
                val query = Query(Criteria.where("sha256hash").`is`(sha256hash))
                mongoTemplate.findOne(query, TranslatedLyric::class.java, COLLECTION_NAME)
            }.fold(
                onSuccess = { translatedLyric ->
                    logger.debug("findBySha256Hash --> Found translated lyric with sha256hash: $sha256hash: ${translatedLyric != null}")
                    emit(Resource.Success(translatedLyric))
                },
                onFailure = { e ->
                    logger.error("findBySha256Hash --> Error finding translated lyric by sha256hash: $sha256hash", e)
                    emit(Resource.Error("Failed to find translated lyric by sha256hash: ${e.message}", e as? Exception))
                },
            )
            logger.debug("findBySha256Hash --> Completed for hash: $sha256hash")
        }.flowOn(Dispatchers.IO)

    override fun save(translatedLyric: TranslatedLyric): Flow<Resource<TranslatedLyric>> =
        flow {
            logger.debug("save started for translated lyric id: ${translatedLyric.id}")
            runCatching {
                logger.debug("Calling mongoTemplate.save for translated lyric with id: ${translatedLyric.id}")
                mongoTemplate.save(translatedLyric, COLLECTION_NAME)
            }.fold(
                onSuccess = { savedTranslatedLyric ->
                    logger.debug("Successfully saved translated lyric with id: ${translatedLyric.id}")
                    emit(Resource.Success(savedTranslatedLyric))
                },
                onFailure = { e ->
                    logger.error("Error saving translated lyric with id: ${translatedLyric.id}", e)
                    emit(Resource.Error("Failed to save translated lyric: ${e.message}", e as? Exception))
                },
            )
            logger.debug("save completed for translated lyric id: ${translatedLyric.id}")
        }.flowOn(Dispatchers.IO)

    override fun updateVote(
        id: String,
        voteIncrement: Int,
    ): Flow<Resource<TranslatedLyric>> =
        flow {
            logger.debug("updateVote --> Started for translated lyric id: $id with increment: $voteIncrement")
            runCatching {
                val query = Query(Criteria.where("id").`is`(id))
                val update = Update().inc("vote", voteIncrement)

                logger.debug("updateVote --> Updating vote count by $voteIncrement for id: $id")
                mongoTemplate.updateFirst(query, update, TranslatedLyric::class.java, COLLECTION_NAME)

                // Fetch updated translated lyric
                mongoTemplate.findById(id, TranslatedLyric::class.java, COLLECTION_NAME)
                    ?: throw RuntimeException("Translated lyric not found after update with id: $id")
            }.fold(
                onSuccess = { updatedTranslatedLyric ->
                    logger.debug("updateVote --> Successfully updated vote for translated lyric id: $id")
                    emit(Resource.Success(updatedTranslatedLyric))
                },
                onFailure = { e ->
                    logger.error("updateVote --> Error updating vote for translated lyric id: $id", e)
                    if (e.message?.contains("not found") == true) {
                        emit(Resource.notFoundError("Translated lyric not found with id: $id"))
                    } else {
                        emit(Resource.Error("Failed to update vote: ${e.message}", e as? Exception))
                    }
                },
            )
            logger.debug("updateVote --> Completed for translated lyric id: $id")
        }.flowOn(Dispatchers.IO)
}
