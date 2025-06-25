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
    @Qualifier("lyricsCollectionId") private val lyricsCollectionId: String
) {
    private val logger = LoggerFactory.getLogger(AppwriteDataSource::class.java)
    
    /**
     * Initialize Appwrite database and collections if they don't exist
     */
    fun initializeAppwrite(): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        
        // Check if database exists, create if it doesn't
        try {
            databases.get(databaseId)
            logger.info("Database $databaseId already exists")
        } catch (e: AppwriteException) {
            if (e.code == 404) {
                databases.create(databaseId, "LyricsDatabase")
                logger.info("Created database $databaseId")
            } else {
                logger.error("Error checking database: ${e.message}")
                throw e
            }
        }
        
        // Check if lyrics collection exists, create if it doesn't
        try {
            databases.getCollection(databaseId, lyricsCollectionId)
            logger.info("Collection $lyricsCollectionId already exists")
        } catch (e: AppwriteException) {
            if (e.code == 404) {
                databases.createCollection(
                    databaseId = databaseId,
                    collectionId = lyricsCollectionId,
                    name = "Lyrics"
                )
                logger.info("Created collection $lyricsCollectionId")
                
                // Create needed attributes for the collection and wait for result
                createLyricsCollectionAttributes().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            logger.info("Successfully created all attributes and indexes")
                        }
                        is Resource.Error -> {
                            throw Exception(result.message, result.exception)
                        }
                        is Resource.Loading -> {
                            // Ignore loading state
                        }
                    }
                }
            } else {
                logger.error("Error checking collection: ${e.message}")
                throw e
            }
        }
        
        emit(Resource.Success("Appwrite initialized successfully"))
    }.catch { e ->
        logger.error("Failed to initialize Appwrite: ${e.message}", e)
        emit(Resource.Error("Failed to initialize Appwrite: ${e.message}", e as? Exception))
    }.flowOn(Dispatchers.IO)
    
    /**
     * Log all properties of an object using reflection
     */
    private fun logAllProperties(obj: Any, objName: String) {
        try {
            val properties = obj::class.members.joinToString(", ") { it.name }
            logger.info("$objName available properties: $properties")
            
            obj::class.members.forEach { member ->
                try {
                    if (member.name != "equals" && member.name != "hashCode" && member.name != "toString") {
                        logger.info("$objName ${member.name}: ${member.call(obj)}")
                    }
                } catch (e: Exception) {
                    logger.info("$objName ${member.name}: [Could not access value]")
                }
            }
        } catch (e: Exception) {
            logger.error("Error logging properties: ${e.message}")
        }
    }
    
    /**
     * Create all required attributes for the Lyrics collection
     */
    private fun createLyricsCollectionAttributes(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        
        try {
            // Required: id, videoId, songTitle, artistName attributes
            val idAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "id",
                size = 36,
                required = true
            )
            logger.info("Created id attribute: ${idAttribute.key}")
            logger.info("ID attribute details: $idAttribute")
            logAllProperties(idAttribute, "ID Attribute")
            
            val videoIdAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "videoId",
                size = 20,
                required = true
            )
            logger.info("Created videoId attribute: ${videoIdAttribute.key}")
            
            val songTitleAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "songTitle",
                size = 255,
                required = true
            )
            logger.info("Created songTitle attribute: ${songTitleAttribute.key}")
            
            val artistNameAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "artistName",
                size = 255,
                required = true
            )
            logger.info("Created artistName attribute: ${artistNameAttribute.key}")
            
            // Optional attributes
            val albumNameAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "albumName",
                size = 255,
                required = false
            )
            logger.info("Created albumName attribute: ${albumNameAttribute.key}")
            
            val durationAttribute = databases.createIntegerAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "durationSeconds",
                required = false,
                min = 0,
                max = 36000
            )
            logger.info("Created durationSeconds attribute: ${durationAttribute.key}")
            
            val plainLyricAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "plainLyric",
                size = 100000, // Very large text
                required = false
            )
            logger.info("Created plainLyric attribute: ${plainLyricAttribute.key}")
            
            val syncedLyricsAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "syncedLyrics",
                size = 100000, // Very large text
                required = false
            )
            logger.info("Created syncedLyrics attribute: ${syncedLyricsAttribute.key}")
            
            val richSyncLyricsAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "richSyncLyrics",
                size = 100000, // Very large text
                required = false
            )
            logger.info("Created richSyncLyrics attribute: ${richSyncLyricsAttribute.key}")
            
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
            
            val contributorAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "contributor",
                size = 255,
                required = false
            )
            logger.info("Created contributor attribute: ${contributorAttribute.key}")
            
            val contributorEmailAttribute = databases.createStringAttribute(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "contributorEmail",
                size = 255,
                required = false
            )
            logger.info("Created contributorEmail attribute: ${contributorEmailAttribute.key}")
            
            // Create indexes for faster queries
            val songTitleIndex = databases.createIndex(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "song_title_idx",
                type = IndexType.FULLTEXT,
                attributes = listOf("songTitle")
            )
            logger.info("Created songTitle index: ${songTitleIndex.key}")
            logger.info("SongTitle index details: $songTitleIndex")
            logAllProperties(songTitleIndex, "SongTitle Index")
            
            val artistIndex = databases.createIndex(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "artist_idx",
                type = IndexType.FULLTEXT,
                attributes = listOf("artistName")
            )
            logger.info("Created artist index: ${artistIndex.key}")
            
            val videoIdIndex = databases.createIndex(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                key = "video_id_idx",
                type = IndexType.KEY,
                attributes = listOf("videoId")
            )
            logger.info("Created videoId index: ${videoIdIndex.key}")
            
            logger.info("Created all attributes and indexes for collection $lyricsCollectionId")
            emit(Resource.Success(true))
        } catch (e: AppwriteException) {
            logger.error("Failed to create collection attributes: ${e.message}", e)
            emit(Resource.Error("Failed to create collection attributes: ${e.message}", e as? Exception))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clear all data from the lyrics collection
     */
    fun clearAllLyrics(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        
        val documents = databases.listDocuments(
            databaseId = databaseId,
            collectionId = lyricsCollectionId
        )
        
        for (doc in documents.documents) {
            databases.deleteDocument(
                databaseId = databaseId,
                collectionId = lyricsCollectionId,
                documentId = doc.id
            )
        }
        
        emit(Resource.Success(true))
    }.catch { e ->
        logger.error("Failed to clear all lyrics: ${e.message}", e)
        emit(Resource.Error("Failed to clear all lyrics: ${e.message}", e as? Exception))
    }.flowOn(Dispatchers.IO)
    
    /**
     * Rebuild database and collections from scratch (dangerous operation)
     */
    fun rebuildDatabase(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        
        try {
            databases.delete(databaseId)
            logger.info("Deleted database $databaseId")
        } catch (e: AppwriteException) {
            if (e.code != 404) {
                throw e
            }
        }
        
        // Recreate everything
        var success = false
        initializeAppwrite().collect { resource ->
            when (resource) {
                is Resource.Success -> success = true
                is Resource.Error -> throw Exception(resource.message, resource.exception)
                else -> {}
            }
        }
        
        emit(Resource.Success(success))
    }.catch { e ->
        logger.error("Failed to rebuild database: ${e.message}", e)
        emit(Resource.Error("Failed to rebuild database: ${e.message}", e as? Exception))
    }.flowOn(Dispatchers.IO)
} 