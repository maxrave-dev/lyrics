package org.simpmusic.lyrics.application.service

import kotlinx.coroutines.flow.Flow
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.infrastructure.datasource.LyricSearchDocument
import org.simpmusic.lyrics.infrastructure.datasource.MeilisearchDataSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Application service for Meilisearch operations
 * Delegates to MeilisearchDataSource for actual implementation
 */
@Service
class MeilisearchService(
    private val meilisearchDataSource: MeilisearchDataSource,
) {
    private val logger = LoggerFactory.getLogger(MeilisearchService::class.java)

    /**
     * Initialize Meilisearch index and settings
     */
    fun initializeIndex(): Flow<Resource<String>> {
        logger.debug("initializeIndex --> Delegating to MeilisearchDataSource")
        return meilisearchDataSource.initializeMeilisearch()
    }

    /**
     * Index a lyric document
     */
    fun indexLyric(document: LyricSearchDocument): Flow<Resource<String>> {
        logger.debug("indexLyric --> Indexing lyric with id: ${document.id}")
        return meilisearchDataSource.indexLyric(document)
    }

    /**
     * Search for lyrics
     */
    fun searchLyrics(
        query: String,
        limit: Int? = null,
        offset: Int? = null,
    ): Flow<Resource<List<LyricSearchDocument>>> {
        logger.debug("searchLyrics --> Searching with query: $query")
        return meilisearchDataSource.searchLyrics(query, limit, offset)
    }

    /**
     * Delete a lyric from the search index
     */
    fun deleteLyric(id: String): Flow<Resource<String>> {
        logger.debug("deleteLyric --> Deleting lyric with id: $id")
        return meilisearchDataSource.deleteLyric(id)
    }

    /**
     * Clear all lyrics from the search index
     */
    fun clearAllLyrics(): Flow<Resource<String>> {
        logger.debug("clearAllLyrics --> Clearing all lyrics from search index")
        return meilisearchDataSource.clearAllLyrics()
    }

    /**
     * Rebuild the entire search index
     */
    fun rebuildIndex(): Flow<Resource<String>> {
        logger.debug("rebuildIndex --> Rebuilding search index")
        return meilisearchDataSource.rebuildIndex()
    }
}
