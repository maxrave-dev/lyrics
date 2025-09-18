package org.simpmusic.lyrics.infrastructure.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.simpmusic.lyrics.domain.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

/**
 * Data source for handling MongoDB-specific operations
 */
@Component
class MongoDataSource(
    private val mongoTemplate: MongoTemplate,
) {
    private val logger = LoggerFactory.getLogger(MongoDataSource::class.java)

    /**
     * Check MongoDB connection
     */
    fun checkConnection(): Flow<Resource<String>> =
        flow {
            logger.info("=== Checking MongoDB connection ===")
            runCatching {
                // Just check if MongoDB connection is working
                mongoTemplate.collectionNames
                logger.info("MongoDB connection established successfully")
                "MongoDB connection verified successfully"
            }.fold(
                onSuccess = { message ->
                    logger.info("=== MongoDB connection check completed successfully ===")
                    emit(Resource.Success(message))
                },
                onFailure = { e ->
                    logger.error("=== MongoDB connection check failed ===", e)
                    emit(Resource.Error("Failed to connect to MongoDB: ${e.message}", e as? Exception))
                },
            )
        }.flowOn(Dispatchers.IO)
}
