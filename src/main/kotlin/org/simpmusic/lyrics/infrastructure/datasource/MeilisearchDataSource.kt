@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.simpmusic.lyrics.infrastructure.datasource

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.SearchRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.simpmusic.lyrics.domain.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Serializable
data class LyricSearchDocument(
    val id: String,
    val videoId: String,
    val songTitle: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
)

/**
 * Data source for handling Meilisearch-specific operations
 */
@Component
class MeilisearchDataSource(
    private val client: Client,
) {
    private val logger = LoggerFactory.getLogger(MeilisearchDataSource::class.java)
    private val indexName = "lyrics_idx"

    /**
     * Initialize Meilisearch index and settings if they don't exist
     */
    fun initializeMeilisearch(): Flow<Resource<String>> =
        flow {
            logger.info("=== Starting Meilisearch initialization ===")
            runCatching {
                logger.info("Step 1: Checking if Meilisearch index exists: $indexName")

                // Check if index already exists
                val indexes = client.indexes
                val indexExists = indexes.results.any { it.uid == indexName }

                if (indexExists) {
                    logger.info("Meilisearch index $indexName already exists")
                    logger.info("=== Meilisearch initialization completed successfully ===")
                    return@runCatching "Meilisearch index already exists"
                }

                logger.info("Step 2: Creating Meilisearch index: $indexName")
                val index = client.index(indexName)

                logger.info("Step 3: Setting up searchable attributes...")
                // Set searchable attributes
                index.updateSearchableAttributesSettings(
                    arrayOf("songTitle", "artistName", "albumName", "plainLyric", "contributor"),
                )
                logger.info("Successfully set searchable attributes")

                logger.info("Step 4: Setting up filterable attributes...")
                // Set filterable attributes
                index.updateFilterableAttributesSettings(
                    arrayOf("videoId", "artistName", "albumName"),
                )
                logger.info("Successfully set filterable attributes")

                logger.info("=== Meilisearch initialization completed successfully ===")
                "Meilisearch index initialized successfully"
            }.fold(
                onSuccess = { message ->
                    logger.info("Meilisearch initialization completed: $message")
                    emit(Resource.Success(message))
                },
                onFailure = { e ->
                    logger.error("=== Meilisearch initialization failed ===", e)
                    emit(Resource.Error("Failed to initialize Meilisearch: ${e.message}", e as? Exception))
                },
            )
        }.flowOn(Dispatchers.IO)

    /**
     * Index a lyric document in Meilisearch
     */
    fun indexLyric(document: LyricSearchDocument): Flow<Resource<String>> =
        flow {
            runCatching {
                logger.debug("indexLyric --> Indexing lyric with id: ${document.id}")

                val index = client.index(indexName)
                index.addDocuments(
//                    videoId,songTitle,artistName,albumName,durationSeconds
                    $$"""
                    [
                        {
                            "$id": "$${document.id}",
                            "videoId": "$${document.videoId}",
                            "songTitle": "$${document.songTitle}",
                            "artistName": "$${document.artistName}",
                            "albumName": "$${document.albumName}"
                            durationSeconds: $${document.durationSeconds},
                        }
                    ]
                    """.trimIndent(),
                    "\$id",
                )

                logger.debug("indexLyric --> Successfully indexed lyric with id: ${document.id}")
                "Lyric indexed successfully"
            }.fold(
                onSuccess = { message ->
                    emit(Resource.Success(message))
                },
                onFailure = { e ->
                    logger.error("indexLyric --> Error indexing lyric with id: ${document.id}", e)
                    emit(Resource.Error("Failed to index lyric: ${e.message}", e as? Exception))
                },
            )
        }.flowOn(Dispatchers.IO)

    /**
     * Search for lyrics in Meilisearch
     */
    fun searchLyrics(
        query: String,
        limit: Int? = null,
        offset: Int? = null,
        filter: String? = null,
    ): Flow<Resource<List<String>>> =
        flow {
            runCatching {
                logger.debug("searchLyrics --> Searching with query: $query, limit: $limit, offset: $offset, filter: $filter")

                val index = client.index(indexName)

                val searchRequest =
                    SearchRequest(query).apply {
                        this.limit = limit ?: 20
                        this.offset = offset ?: 0
                        filter?.let { this.filter = arrayOf(it) }
                        this.attributesToRetrieve = arrayOf("id")
                    }

                val searchResult = index.search(searchRequest)
                val lyricIds =
                    searchResult.hits.mapNotNull { hit ->
                        when (hit) {
                            is JsonObject -> hit["id"]?.jsonPrimitive?.content
                            else -> null
                        }
                    }

                logger.debug("searchLyrics --> Found ${lyricIds.size} results for query: $query")
                lyricIds
            }.fold(
                onSuccess = { lyricIds ->
                    emit(Resource.Success(lyricIds))
                },
                onFailure = { e ->
                    logger.error("searchLyrics --> Error searching lyrics with query: $query", e)
                    emit(Resource.Error("Failed to search lyrics: ${e.message}", e as? Exception))
                },
            )
        }.flowOn(Dispatchers.IO)

    /**
     * Delete a lyric document from Meilisearch index
     */
    fun deleteLyric(id: String): Flow<Resource<String>> =
        flow {
            runCatching {
                logger.debug("deleteLyric --> Deleting lyric from search index with id: $id")

                val index = client.index(indexName)
                index.deleteDocument(id)

                logger.debug("deleteLyric --> Successfully deleted lyric from search index with id: $id")
                "Lyric deleted from search index"
            }.fold(
                onSuccess = { message ->
                    emit(Resource.Success(message))
                },
                onFailure = { e ->
                    logger.error("deleteLyric --> Error deleting lyric from search index with id: $id", e)
                    emit(Resource.Error("Failed to delete lyric from search index: ${e.message}", e as? Exception))
                },
            )
        }.flowOn(Dispatchers.IO)

    /**
     * Clear all documents from the search index
     */
    fun clearAllLyrics(): Flow<Resource<String>> =
        flow {
            runCatching {
                logger.info("clearAllLyrics --> Starting to clear all documents from search index")

                val index = client.index(indexName)
                index.deleteAllDocuments()

                logger.info("clearAllLyrics --> Successfully cleared all documents from search index")
                "All lyrics cleared from search index"
            }.fold(
                onSuccess = { message ->
                    emit(Resource.Success(message))
                },
                onFailure = { e ->
                    logger.error("clearAllLyrics --> Error clearing search index", e)
                    emit(Resource.Error("Failed to clear search index: ${e.message}", e as? Exception))
                },
            )
        }.flowOn(Dispatchers.IO)

    /**
     * Rebuild the search index (delete and recreate)
     */
    fun rebuildIndex(): Flow<Resource<String>> =
        flow {
            runCatching {
                logger.info("rebuildIndex --> Starting to rebuild search index")

                // Delete the index
                try {
                    client.deleteIndex(indexName)
                    logger.info("rebuildIndex --> Deleted existing index: $indexName")
                } catch (e: Exception) {
                    logger.info("rebuildIndex --> Index $indexName doesn't exist or couldn't be deleted, proceeding to create")
                }

                // Recreate the index
                var success = false
                initializeMeilisearch().collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            success = true
                            logger.info("rebuildIndex --> Successfully rebuilt search index")
                        }

                        is Resource.Error -> {
                            logger.error("rebuildIndex --> Failed to rebuild index: ${resource.message}")
                            throw Exception(resource.message, resource.exception)
                        }

                        else -> {
                            logger.debug("rebuildIndex --> Rebuild in progress...")
                        }
                    }
                }

                "Search index rebuilt successfully"
            }.fold(
                onSuccess = { message ->
                    emit(Resource.Success(message))
                },
                onFailure = { e ->
                    logger.error("rebuildIndex --> Error rebuilding search index", e)
                    emit(Resource.Error("Failed to rebuild search index: ${e.message}", e as? Exception))
                },
            )
        }.flowOn(Dispatchers.IO)
}
