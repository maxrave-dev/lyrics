package org.simpmusic.lyrics.infrastructure.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.simpmusic.lyrics.domain.model.Lyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

/**
 * MongoDB implementation of the LyricRepository
 */
@Repository("mongoLyricRepositoryImpl")
class MongoLyricRepository(
    private val mongoTemplate: MongoTemplate,
) : LyricRepository {
    private val logger = LoggerFactory.getLogger(MongoLyricRepository::class.java)

    companion object {
        private const val COLLECTION_NAME = "lyrics"
    }

    override fun findById(id: String): Flow<Resource<Lyric?>> =
        flow {
            logger.debug("findById started for id: $id")
            runCatching {
                logger.debug("Calling mongoTemplate.findById for id: $id")
                mongoTemplate.findById(id, Lyric::class.java, COLLECTION_NAME)
            }.fold(
                onSuccess = { lyric ->
                    logger.debug("Successfully found document for id: $id")
                    emit(Resource.Success(lyric))
                },
                onFailure = { e ->
                    logger.error("Failed to find document for id: $id, error: ${e.message}", e)
                    emit(Resource.Error("Failed to find lyric: ${e.message}", e as? Exception))
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
            runCatching {
                val query = Query(Criteria.where("songTitle").regex(title, "i"))

                offset?.let { query.skip(it.toLong()) }
                limit?.let { query.limit(it) }

                logger.debug("Calling mongoTemplate.find with title query: $title")
                mongoTemplate.find(query, Lyric::class.java, COLLECTION_NAME)
            }.fold(
                onSuccess = { lyrics ->
                    logger.debug("Successfully found ${lyrics.size} documents for title: $title")
                    emit(Resource.Success(lyrics))
                },
                onFailure = { e ->
                    logger.error("Error finding lyrics by title: $title", e)
                    emit(Resource.Error("Failed to find lyrics by title: ${e.message}", e as? Exception))
                },
            )
            logger.debug("findBySongTitle completed for title: $title")
        }.flowOn(Dispatchers.IO)

    override fun findByVideoId(
        videoId: String,
        limit: Int?,
        offset: Int?,
    ): Flow<Resource<List<Lyric>>> =
        flow {
            logger.debug("findByVideoId started for videoId: $videoId, limit: $limit, offset: $offset")
            runCatching {
                val query = Query(Criteria.where("videoId").`is`(videoId))

                offset?.let { query.skip(it.toLong()) }
                limit?.let { query.limit(it) }

                logger.debug("Calling mongoTemplate.find with videoId query: {}", query)
                mongoTemplate.find(query, Lyric::class.java, COLLECTION_NAME)
            }.fold(
                onSuccess = { lyrics ->
                    logger.debug("Successfully found ${lyrics.size} documents for videoId: $videoId")
                    emit(Resource.Success(lyrics))
                },
                onFailure = { e ->
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
            runCatching {
                val criteria =
                    Criteria().orOperator(
                        Criteria.where("songTitle").regex(keywords, "i"),
                        Criteria.where("artistName").regex(keywords, "i"),
                    )
                val query = Query(criteria)

                offset?.let { query.skip(it.toLong()) }
                limit?.let { query.limit(it) }

                logger.debug("Calling mongoTemplate.find with search query: $keywords")
                mongoTemplate.find(query, Lyric::class.java, COLLECTION_NAME)
            }.fold(
                onSuccess = { lyrics ->
                    logger.debug("Successfully found ${lyrics.size} documents for search: $keywords")
                    emit(Resource.Success(lyrics))
                },
                onFailure = { e ->
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
                logger.debug("findBySha256Hash --> Calling mongoTemplate.findOne with sha256hash query: $sha256hash")
                val query = Query(Criteria.where("sha256hash").`is`(sha256hash))
                mongoTemplate.findOne(query, Lyric::class.java, COLLECTION_NAME)
            }.fold(
                onSuccess = { lyric ->
                    logger.debug("findBySha256Hash --> Found lyric with sha256hash: $sha256hash: ${lyric != null}")
                    emit(Resource.Success(lyric))
                },
                onFailure = { e ->
                    logger.error("findBySha256Hash --> Error finding lyric by sha256hash: $sha256hash", e)
                    emit(Resource.Error("Failed to find lyric by sha256hash: ${e.message}", e as? Exception))
                },
            )
            logger.debug("findBySha256Hash --> Completed for hash: $sha256hash")
        }.flowOn(Dispatchers.IO)

    override fun save(lyric: Lyric): Flow<Resource<Lyric>> =
        flow {
            logger.debug("save started for lyric id: ${lyric.id}")
            runCatching {
                logger.debug("Calling mongoTemplate.save for lyric id: ${lyric.id}")
                mongoTemplate.save(lyric, COLLECTION_NAME)
            }.fold(
                onSuccess = { savedLyric ->
                    logger.debug("Successfully saved lyric with id: ${lyric.id}")
                    emit(Resource.Success(savedLyric))
                },
                onFailure = { e ->
                    logger.error("Error saving lyric with id: ${lyric.id}", e)
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
                val query = Query(Criteria.where("id").`is`(id))
                val update = Update().inc("vote", voteIncrement)

                logger.debug("updateVote --> Updating vote count by $voteIncrement for id: $id")
                mongoTemplate.updateFirst(query, update, Lyric::class.java, COLLECTION_NAME)

                // Fetch updated lyric
                mongoTemplate.findById(id, Lyric::class.java, COLLECTION_NAME)
                    ?: throw RuntimeException("Lyric not found after update with id: $id")
            }.fold(
                onSuccess = { updatedLyric ->
                    logger.debug("updateVote --> Successfully updated vote for id: $id")
                    emit(Resource.Success(updatedLyric))
                },
                onFailure = { e ->
                    logger.error("updateVote --> Error updating vote for id: $id", e)
                    if (e.message?.contains("not found") == true) {
                        emit(Resource.notFoundError("Lyric not found with id: $id"))
                    } else {
                        emit(Resource.Error("Failed to update vote: ${e.message}", e as? Exception))
                    }
                },
            )
            logger.debug("updateVote --> Completed for id: $id")
        }.flowOn(Dispatchers.IO)
}
