package org.simpmusic.lyrics.infrastructure.datasource

import io.appwrite.enums.IndexType
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.simpmusic.lyrics.domain.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import kotlin.reflect.full.memberProperties

/**
 * Data source for handling Appwrite-specific operations
 */
@Component
class AppwriteDataSource(
    private val databases: Databases,
    @Qualifier("databaseId") private val databaseId: String,
    @Qualifier("lyricsCollectionId") private val lyricsCollectionId: String,
    @Qualifier("translatedLyricsCollectionId") private val translatedLyricsCollectionId: String,
    @Qualifier("notFoundLyricsCollectionId") private val notFoundLyricsCollectionId: String,
    private val appwriteCollectionInitializer: AppwriteCollectionInitializer
) {
    private val logger = LoggerFactory.getLogger(AppwriteDataSource::class.java)
    
    /**
     * Initialize Appwrite database and collections if they don't exist
     */
    fun initializeAppwrite(): Flow<Resource<String>> = flow {
        logger.info("=== Starting Appwrite initialization ===")
        emit(Resource.Loading)
        
        runCatching {
            logger.info("Step 1: Checking if database exists: $databaseId")
            
            // Check if database exists, create if it doesn't
            var databaseExists = false
            try {
                databases.get(databaseId)
                databaseExists = true
                logger.info("Database $databaseId already exists")
            } catch (e: AppwriteException) {
                if (e.code == 404) {
                    logger.info("Database $databaseId doesn't exist, creating...")
                    databases.create(databaseId, "LyricsDatabase")
                    logger.info("Successfully created database $databaseId")
                    databaseExists = true
                } else {
                    logger.error("Error checking database: ${e.message}")
                    throw e
                }
            }
            
            logger.info("Step 2: Checking if collection exists: $lyricsCollectionId")
            
            // Check if lyrics collection exists, create if it doesn't
            var collectionExists = false
            try {
                databases.getCollection(databaseId, lyricsCollectionId)
                collectionExists = true
                logger.info("Collection $lyricsCollectionId already exists")
            } catch (e: AppwriteException) {
                if (e.code == 404) {
                    logger.info("Collection $lyricsCollectionId doesn't exist, creating...")
                    databases.createCollection(
                        databaseId = databaseId,
                        collectionId = lyricsCollectionId,
                        name = "Lyrics"
                    )
                    logger.info("Successfully created collection $lyricsCollectionId")
                    
                    logger.info("Step 3: Creating collection attributes...")
                    // Create needed attributes for the collection and wait for result
                    createLyricsCollectionAttributes().collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                logger.info("Successfully created all attributes and indexes")
                            }
                            is Resource.Error -> {
                                logger.error("Failed to create attributes: ${result.message}")
                                throw Exception(result.message, result.exception)
                            }
                            is Resource.Loading -> {
                                logger.debug("Creating attributes in progress...")
                            }
                        }
                    }
                    collectionExists = true
                } else {
                    logger.error("Error checking collection: ${e.message}")
                    throw e
                }
            }
            
            // Check and create translated_lyrics collection
            logger.info("Step 4: Checking if translated_lyrics collection exists: $translatedLyricsCollectionId")
            try {
                databases.getCollection(databaseId, translatedLyricsCollectionId)
                logger.info("Collection $translatedLyricsCollectionId already exists")
            } catch (e: AppwriteException) {
                if (e.code == 404) {
                    logger.info("Collection $translatedLyricsCollectionId doesn't exist, creating...")
                    databases.createCollection(
                        databaseId = databaseId,
                        collectionId = translatedLyricsCollectionId,
                        name = "TranslatedLyrics"
                    )
                    logger.info("Successfully created collection $translatedLyricsCollectionId")
                    
                    // Create attributes for translated_lyrics collection
                    appwriteCollectionInitializer.createTranslatedLyricsCollectionAttributes().collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                logger.info("Successfully created all translated_lyrics attributes")
                            }
                            is Resource.Error -> {
                                logger.error("Failed to create translated_lyrics attributes: ${result.message}")
                                throw Exception(result.message, result.exception)
                            }
                            is Resource.Loading -> {
                                logger.debug("Creating translated_lyrics attributes in progress...")
                            }
                        }
                    }
                } else {
                    logger.error("Error checking translated_lyrics collection: ${e.message}")
                    throw e
                }
            }
            
            // Check and create notfound_lyrics collection
            logger.info("Step 5: Checking if notfound_lyrics collection exists: $notFoundLyricsCollectionId")
            try {
                databases.getCollection(databaseId, notFoundLyricsCollectionId)
                logger.info("Collection $notFoundLyricsCollectionId already exists")
            } catch (e: AppwriteException) {
                if (e.code == 404) {
                    logger.info("Collection $notFoundLyricsCollectionId doesn't exist, creating...")
                    databases.createCollection(
                        databaseId = databaseId,
                        collectionId = notFoundLyricsCollectionId,
                        name = "NotFoundLyrics"
                    )
                    logger.info("Successfully created collection $notFoundLyricsCollectionId")
                    
                    // Create attributes for notfound_lyrics collection
                    appwriteCollectionInitializer.createNotFoundLyricsCollectionAttributes().collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                logger.info("Successfully created all notfound_lyrics attributes")
                            }
                            is Resource.Error -> {
                                logger.error("Failed to create notfound_lyrics attributes: ${result.message}")
                                throw Exception(result.message, result.exception)
                            }
                            is Resource.Loading -> {
                                logger.debug("Creating notfound_lyrics attributes in progress...")
                            }
                        }
                    }
                } else {
                    logger.error("Error checking notfound_lyrics collection: ${e.message}")
                    throw e
                }
            }
            
            logger.info("Database and all collections setup completed successfully")
            "Appwrite initialized successfully"
            
        }.fold(
            onSuccess = { message ->
                logger.info("=== Appwrite initialization completed successfully ===")
                emit(Resource.Success(message))
            },
            onFailure = { e ->
                logger.error("=== Appwrite initialization failed ===", e)
                emit(Resource.Error("Failed to initialize Appwrite: ${e.message}", e as? Exception))
            }
        )
    }.flowOn(Dispatchers.IO)
    
    /**
     * Log all properties of an object using reflection
     */
    private fun logAllProperties(obj: Any, objName: String) {
        runCatching {
            val properties = obj::class.members.joinToString(", ") { it.name }
            logger.info("$objName available properties: $properties")
            
            obj::class.members.forEach { member ->
                runCatching {
                    if (member.name != "equals" && member.name != "hashCode" && member.name != "toString") {
                        logger.info("$objName ${member.name}: ${member.call(obj)}")
                    }
                }.getOrElse {
                    logger.info("$objName ${member.name}: [Could not access value]")
                }
            }
        }.getOrElse { e ->
            logger.error("Error logging properties: ${e.message}")
        }
    }
    
    /**
     * Create all required attributes for the Lyrics collection
     */
    private fun createLyricsCollectionAttributes(): Flow<Resource<Boolean>> = flow {
        logger.info("--- Starting attribute creation ---")
        emit(Resource.Loading)
        
        runCatching {
            logger.info("Creating id attribute...")
            val idAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "id",
                size = 64,
                required = true
            )
            logger.info("Created id attribute: ${idAttribute.key}")
            
            logger.info("Creating videoId attribute...")
            val videoIdAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "videoId",
                size = 20,
                required = true
            )
            logger.info("Created videoId attribute: ${videoIdAttribute.key}")
            
            logger.info("Creating songTitle attribute...")
            val songTitleAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "songTitle",
                size = 255,
                required = true
            )
            logger.info("Created songTitle attribute: ${songTitleAttribute.key}")
            
            logger.info("Creating artistName attribute...")
            val artistNameAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "artistName",
                size = 255,
                required = true
            )
            logger.info("Created artistName attribute: ${artistNameAttribute.key}")
            
            logger.info("Creating albumName attribute...")
            val albumNameAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "albumName",
                size = 255,
                required = false
            )
            logger.info("Created albumName attribute: ${albumNameAttribute.key}")
            
            logger.info("Creating durationSeconds attribute...")
            val durationAttribute = databases.createIntegerAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "durationSeconds",
                required = false,
                min = 0,
                max = 36000
            )
            logger.info("Created durationSeconds attribute: ${durationAttribute.key}")
            
            logger.info("Creating plainLyric attribute...")
            val plainLyricAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "plainLyric",
                size = 100000, // Very large text
                required = false
            )
            logger.info("Created plainLyric attribute: ${plainLyricAttribute.key}")
            
            logger.info("Creating syncedLyrics attribute...")
            val syncedLyricsAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "syncedLyrics",
                size = 100000, // Very large text
                required = false
            )
            logger.info("Created syncedLyrics attribute: ${syncedLyricsAttribute.key}")
            
            logger.info("Creating richSyncLyrics attribute...")
            val richSyncLyricsAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "richSyncLyrics",
                size = 100000, // Very large text
                required = false
            )
            logger.info("Created richSyncLyrics attribute: ${richSyncLyricsAttribute.key}")
            
            logger.info("Creating vote attribute...")
            val voteAttribute = databases.createIntegerAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "vote",
                required = false,
                min = -999999,
                max = 999999,
                default = 0
            )
            logger.info("Created vote attribute: ${voteAttribute.key}")
            
            logger.info("Creating contributor attribute...")
            val contributorAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "contributor",
                size = 255,
                required = false
            )
            logger.info("Created contributor attribute: ${contributorAttribute.key}")
            
            logger.info("Creating contributorEmail attribute...")
            val contributorEmailAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "contributorEmail",
                size = 255,
                required = false
            )
            logger.info("Created contributorEmail attribute: ${contributorEmailAttribute.key}")

            // Create sha256hash attribute
            logger.info("Creating sha256hash attribute...")
            val sha256hashAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "sha256hash",
                size = 64,
                required = true
            )
            logger.info("Created sha256hash attribute: ${sha256hashAttribute.key}")
            
            logger.info("Creating indexes...")
            
            logger.info("Creating songTitle index...")
            val songTitleIndex = databases.createIndex(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "song_title_idx",
                type = IndexType.FULLTEXT,
                attributes = listOf("songTitle")
            )
            logger.info("Created songTitle index: ${songTitleIndex.key}")
            
            logger.info("Creating artist index...")
            val artistIndex = databases.createIndex(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "artist_idx",
                type = IndexType.FULLTEXT,
                attributes = listOf("artistName")
            )
            logger.info("Created artist index: ${artistIndex.key}")
            
            logger.info("Creating videoId index...")
            val videoIdIndex = databases.createIndex(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "video_id_idx",
                type = IndexType.KEY,
                attributes = listOf("videoId")
            )
            logger.info("Created videoId index: ${videoIdIndex.key}")
            
            logger.info("All attributes and indexes created successfully")
            true
            
        }.fold(
            onSuccess = { success ->
                logger.info("--- Attribute creation completed successfully ---")
                emit(Resource.Success(success))
            },
            onFailure = { e ->
                logger.error("--- Attribute creation failed ---", e)
                emit(Resource.Error("Failed to create collection attributes: ${e.message}", e as? Exception))
            }
        )
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clear all data from the lyrics collection
     */
    fun clearAllLyrics(): Flow<Resource<Boolean>> = flow {
        logger.info("Starting clearAllLyrics")
        emit(Resource.Loading)
        
        runCatching {
            logger.info("Fetching all documents from collection")
            val documents = databases.listDocuments(
                databaseId = databaseId,
                collectionId = lyricsCollectionId
            )
            
            logger.info("Found ${documents.documents.size} documents to delete")
            for (doc in documents.documents) {
                logger.debug("Deleting document: ${doc.id}")
                databases.deleteDocument(
                    databaseId = databaseId,
                    collectionId = lyricsCollectionId,
                    documentId = doc.id
                )
            }
            logger.info("All documents deleted successfully")
            true
            
        }.fold(
            onSuccess = { success ->
                logger.info("clearAllLyrics completed successfully")
                emit(Resource.Success(success))
            },
            onFailure = { e ->
                logger.error("clearAllLyrics failed", e)
                emit(Resource.Error("Failed to clear all lyrics: ${e.message}", e as? Exception))
            }
        )
    }.flowOn(Dispatchers.IO)
    
    /**
     * Rebuild database and collections from scratch (dangerous operation)
     */
    fun rebuildDatabase(): Flow<Resource<Boolean>> = flow {
        logger.info("Starting rebuildDatabase")
        emit(Resource.Loading)
        
        runCatching {
            logger.info("Attempting to delete existing database")
            try {
                databases.delete(databaseId)
                logger.info("Deleted database $databaseId")
            } catch (e: AppwriteException) {
                if (e.code == 404) {
                    logger.info("Database $databaseId doesn't exist, proceeding to create")
                } else {
                    logger.error("Error deleting database", e)
                    throw e
                }
            }
            
            logger.info("Recreating everything...")
            var success = false
            initializeAppwrite().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        success = true
                        logger.info("Rebuild completed successfully")
                    }
                    is Resource.Error -> {
                        logger.error("Rebuild failed during initialization: ${resource.message}")
                        throw Exception(resource.message, resource.exception)
                    }
                    else -> {
                        logger.debug("Rebuild in progress...")
                    }
                }
            }
            success
            
        }.fold(
            onSuccess = { success ->
                logger.info("rebuildDatabase completed successfully")
                emit(Resource.Success(success))
            },
            onFailure = { e ->
                logger.error("rebuildDatabase failed", e)
                emit(Resource.Error("Failed to rebuild database: ${e.message}", e as? Exception))
            }
        )
    }.flowOn(Dispatchers.IO)
} 