package org.simpmusic.lyrics.infrastructure.datasource

import io.appwrite.enums.IndexType
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.simpmusic.lyrics.domain.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * Component for initializing additional Appwrite collections
 */
@Component
class AppwriteCollectionInitializer(
    private val databases: Databases,
    @Qualifier("databaseId") private val databaseId: String,
    @Qualifier("translatedLyricsCollectionId") private val translatedLyricsCollectionId: String,
) {
    private val logger = LoggerFactory.getLogger(AppwriteCollectionInitializer::class.java)

    /**
     * Create all required attributes for the TranslatedLyrics collection
     */
    fun createTranslatedLyricsCollectionAttributes(): Flow<Resource<Boolean>> =
        flow {
            logger.info("--- Starting translated_lyrics attribute creation ---")
            runCatching {
                // id attribute (UUID)
                logger.info("Creating id attribute for translated_lyrics...")
                val idAttribute =
                    databases.createStringAttribute(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        key = "id",
                        size = 64,
                        required = true,
                    )
                logger.info("Created id attribute: ${idAttribute.key}")

                // videoId attribute
                logger.info("Creating videoId attribute for translated_lyrics...")
                val videoIdAttribute =
                    databases.createStringAttribute(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        key = "videoId",
                        size = 20,
                        required = true,
                    )
                logger.info("Created videoId attribute: ${videoIdAttribute.key}")

                // translatedLyric attribute (LRC format)
                logger.info("Creating translatedLyric attribute for translated_lyrics...")
                val translatedLyricAttribute =
                    databases.createStringAttribute(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        key = "translatedLyric",
                        size = 65535, // Large size for LRC content
                        required = true,
                    )
                logger.info("Created translatedLyric attribute: ${translatedLyricAttribute.key}")

                // language attribute (2-letter code)
                logger.info("Creating language attribute for translated_lyrics...")
                val languageAttribute =
                    databases.createStringAttribute(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        key = "language",
                        size = 2,
                        required = true,
                    )
                logger.info("Created language attribute: ${languageAttribute.key}")

                // vote attribute
                logger.info("Creating vote attribute for translated_lyrics...")
                val voteAttribute =
                    databases.createIntegerAttribute(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        key = "vote",
                        required = true,
                        min = -999999,
                        max = 999999,
                    )
                logger.info("Created vote attribute: ${voteAttribute.key}")

                // contributor attribute
                logger.info("Creating contributor attribute for translated_lyrics...")
                val contributorAttribute =
                    databases.createStringAttribute(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        key = "contributor",
                        size = 255,
                        required = true,
                    )
                logger.info("Created contributor attribute: ${contributorAttribute.key}")

                // contributorEmail attribute
                logger.info("Creating contributorEmail attribute for translated_lyrics...")
                val contributorEmailAttribute =
                    databases.createStringAttribute(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        key = "contributorEmail",
                        size = 255,
                        required = true,
                    )
                logger.info("Created contributorEmail attribute: ${contributorEmailAttribute.key}")

                // sha256hash attribute
                logger.info("Creating sha256hash attribute for translated_lyrics...")
                val sha256hashAttribute =
                    databases.createStringAttribute(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        key = "sha256hash",
                        size = 64,
                        required = true,
                    )
                logger.info("Created sha256hash attribute: ${sha256hashAttribute.key}")

                // Create indexes
                logger.info("Creating indexes for translated_lyrics...")

                // Index on videoId for fast lookup
                val videoIdIndex =
                    databases.createIndex(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        key = "videoId_index",
                        type = IndexType.KEY,
                        attributes = listOf("videoId"),
                    )
                logger.info("Created videoId index: ${videoIdIndex.key}")

                // Index on language for filtering by language
                val languageIndex =
                    databases.createIndex(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        key = "language_index",
                        type = IndexType.KEY,
                        attributes = listOf("language"),
                    )
                logger.info("Created language index: ${languageIndex.key}")

                logger.info("Successfully created all translated_lyrics attributes and indexes")
                emit(Resource.Success(true))
            }.getOrElse { e ->
                logger.error("Failed to create translated_lyrics attributes: ${e.message}", e)
                emit(Resource.Error("Failed to create translated_lyrics attributes: ${e.message}", e as? Exception))
            }
        }.catch { e ->
            logger.error("Exception in createTranslatedLyricsCollectionAttributes", e)
            emit(Resource.Error("Failed to create translated_lyrics attributes: ${e.message}", e as? Exception))
        }.flowOn(Dispatchers.IO)
}
