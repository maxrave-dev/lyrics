@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.simpmusic.lyrics.application.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import org.simpmusic.lyrics.application.dto.request.*
import org.simpmusic.lyrics.application.dto.response.*
import org.simpmusic.lyrics.domain.model.NotFoundLyric
import org.simpmusic.lyrics.domain.model.NotFoundTranslatedLyric
import org.simpmusic.lyrics.domain.model.Resource
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.domain.repository.NotFoundLyricRepository
import org.simpmusic.lyrics.domain.repository.NotFoundTranslatedLyricRepository
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.simpmusic.lyrics.extensions.*
import org.simpmusic.lyrics.infrastructure.datasource.AppwriteDataSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Application service implementing lyrics use cases
 */
@Service
class LyricService(
    private val lyricRepository: LyricRepository,
    private val translatedLyricRepository: TranslatedLyricRepository,
    private val notFoundLyricRepository: NotFoundLyricRepository,
    private val notFoundTranslatedLyricRepository: NotFoundTranslatedLyricRepository,
    private val appwriteDataSource: AppwriteDataSource,
    private val serviceScope: CoroutineScope,
) {
    private val logger = LoggerFactory.getLogger(LyricService::class.java)

    /**
     * Initialize Appwrite database and collections
     * Uses shareHot to make the flow hot and allow multiple collectors
     */
    fun initializeAppwrite(): Flow<Resource<String>> =
        appwriteDataSource
            .initializeAppwrite()
            .logEach("Appwrite initialization")
            .logCompletion("Appwrite initialization completed")
            .catchToResourceError()
            .shareHot(serviceScope)

    fun getLyricById(id: String): Flow<Resource<LyricResponseDTO?>> =
        lyricRepository
            .findById(id)
            .mapSuccessNotNull { it.toResponseDTO() }
            .catchToResourceError()

    fun getLyricsByVideoId(
        videoId: String,
        limit: Int? = null,
        offset: Int? = null,
    ): Flow<Resource<List<LyricResponseDTO>>> =
        flow {
            emit(Resource.Loading)

            try {
                // First, try to find lyrics
                val lyricsResult = lyricRepository.findByVideoId(videoId, limit, offset).last()

                when (lyricsResult) {
                    is Resource.Success -> {
                        if (lyricsResult.data.isEmpty()) {
                            // No lyrics found, check if already in notfound_lyrics
                            logger.debug("No lyrics found for videoId: $videoId, checking notfound_lyrics...")

                            val notFoundResult = notFoundLyricRepository.findByVideoId(videoId).last()
                            when (notFoundResult) {
                                is Resource.Success if (notFoundResult.data != null) -> {
                                    logger.debug("VideoId: $videoId already exists in notfound_lyrics")
                                }
                                is Resource.Loading -> {
                                }
                                else -> {
                                    logger.info("Adding videoId: $videoId to notfound_lyrics")
                                    val notFoundLyric =
                                        NotFoundLyric(
                                            videoId = videoId,
                                            addedDate = LocalDateTime.now(),
                                        )

                                    // Save to notfound_lyrics
                                    val saveResult = notFoundLyricRepository.save(notFoundLyric).last()
                                    when (saveResult) {
                                        is Resource.Success -> {
                                            logger.info("Successfully saved NotFoundLyric for videoId: $videoId")
                                        }
                                        is Resource.Error -> {
                                            logger.warn("Failed to save NotFoundLyric for videoId: $videoId - ${saveResult.message}")
                                        }
                                        else -> {} // Loading state
                                    }
                                } // Loading state
                            }

                            // Return empty list regardless of notfound_lyrics operation result
                            emit(Resource.Success(emptyList<LyricResponseDTO>()))
                        } else {
                            // Lyrics found, return them
                            val lyricDTOs = lyricsResult.data.map { it.toResponseDTO() }
                            emit(Resource.Success(lyricDTOs))
                        }
                    }
                    is Resource.Error -> {
                        emit(Resource.Error(lyricsResult.message, lyricsResult.exception))
                    }
                    else -> {} // Loading state
                }
            } catch (e: Exception) {
                emit(Resource.Error("Error getting lyrics for videoId: $videoId", e))
            }
        }.flowOn(Dispatchers.IO)

    fun getLyricsBySongTitle(
        title: String,
        limit: Int? = null,
        offset: Int? = null,
    ): Flow<Resource<List<LyricResponseDTO>>> =
        lyricRepository
            .findBySongTitle(title, limit, offset)
            .mapSuccess { lyrics -> lyrics.map { it.toResponseDTO() } }
            .catchToResourceError()

    fun getLyricsByArtist(
        artist: String,
        limit: Int? = null,
        offset: Int? = null,
    ): Flow<Resource<List<LyricResponseDTO>>> =
        lyricRepository
            .findByArtist(artist, limit, offset)
            .mapSuccess { lyrics -> lyrics.map { it.toResponseDTO() } }
            .catchToResourceError()

    fun searchLyrics(
        keywords: String,
        limit: Int? = null,
        offset: Int? = null,
    ): Flow<Resource<List<LyricResponseDTO>>> =
        lyricRepository
            .search(keywords, limit, offset)
            .mapSuccess { lyrics -> lyrics.map { it.toResponseDTO() } }
            .catchToResourceError()

    fun saveLyric(lyricRequestDTO: LyricRequestDTO): Flow<Resource<LyricResponseDTO>> =
        flow {
            emit(Resource.Loading)

            try {
                val lyric = lyricRequestDTO.toEntity()

                // Check for duplicate data based on sha256hash
                logger.debug("saveLyric --> Checking for duplicate lyric with sha256hash: ${lyric.sha256hash}")
                val existingResult = lyricRepository.findBySha256Hash(lyric.sha256hash).last()

                when (existingResult) {
                    is Resource.Success -> {
                        if (existingResult.data != null) {
                            // Lyric with this sha256hash already exists
                            logger.warn("saveLyric --> Duplicate lyric detected with sha256hash: ${lyric.sha256hash}")
                            emit(Resource.duplicateError("This lyrics already exists"))
                            return@flow
                        }
                    }
                    is Resource.Error -> {
                        logger.warn("saveLyric --> Error checking for duplicate lyric: ${existingResult.message}")
                        // Continue to save if unable to check for duplicates
                    }
                    else -> {} // Loading state
                }

                val saveResult = lyricRepository.save(lyric).last()

                when (saveResult) {
                    is Resource.Success -> {
                        // When lyrics are successfully saved, remove from notfound_lyrics if exists
                        logger.debug("Lyric saved successfully, checking if videoId: ${lyricRequestDTO.videoId} exists in notfound_lyrics")

                        try {
                            val deleteResult = notFoundLyricRepository.deleteByVideoId(lyricRequestDTO.videoId).last()
                            when (deleteResult) {
                                is Resource.Success -> {
                                    if (deleteResult.data) {
                                        logger.info("Removed videoId: ${lyricRequestDTO.videoId} from notfound_lyrics after adding lyrics")
                                    }
                                }
                                is Resource.Error -> {
                                    logger.warn(
                                        "Failed to remove videoId: ${lyricRequestDTO.videoId} from notfound_lyrics: ${deleteResult.message}",
                                    )
                                }
                                else -> {} // Loading state
                            }
                        } catch (e: Exception) {
                            logger.warn("Exception while removing videoId: ${lyricRequestDTO.videoId} from notfound_lyrics", e)
                        }

                        emit(Resource.Success(saveResult.data.toResponseDTO()))
                    }
                    is Resource.Error -> {
                        emit(Resource.Error(saveResult.message, saveResult.exception))
                    }
                    else -> {} // Loading state
                }
            } catch (e: Exception) {
                emit(Resource.Error("Error saving lyric", e))
            }
        }.flowOn(Dispatchers.IO)

    /**
     * Vote for a lyric (upvote or downvote)
     * @param voteDTO The vote data transfer object containing the lyric ID and vote value
     * @return A flow of Resource<LyricResponseDTO> representing the updated lyric
     */
    fun voteLyric(voteDTO: VoteRequestDTO): Flow<Resource<LyricResponseDTO>> =
        flow {
            emit(Resource.Loading)
            logger.debug("voteLyric --> Processing vote for lyric id: ${voteDTO.id}")

            try {
                // Convert vote value to increment: 1 for upvote, -1 for downvote
                val voteIncrement = if (voteDTO.vote == 1) 1 else -1
                logger.debug("voteLyric --> Vote increment: $voteIncrement")

                val result = lyricRepository.updateVote(voteDTO.id, voteIncrement).last()

                when (result) {
                    is Resource.Success -> {
                        logger.debug("voteLyric --> Successfully updated vote for lyric id: ${voteDTO.id}")
                        emit(Resource.Success(result.data.toResponseDTO()))
                    }
                    is Resource.Error -> {
                        logger.error("voteLyric --> Failed to update vote for lyric id: ${voteDTO.id}: ${result.message}")
                        emit(Resource.Error(result.message, result.exception, result.code))
                    }
                    else -> {} // Loading state
                }
            } catch (e: Exception) {
                logger.error("voteLyric --> Error processing vote: ${e.message}", e)
                emit(Resource.Error("Failed to process vote: ${e.message}", e))
            }
        }.flowOn(Dispatchers.IO)

    // ========== TranslatedLyric Methods ==========

    fun getTranslatedLyricById(id: String): Flow<Resource<TranslatedLyricResponseDTO?>> =
        translatedLyricRepository
            .findById(id)
            .mapSuccessNotNull { it.toResponseDTO() }
            .catchToResourceError()

    fun getTranslatedLyricsByVideoId(
        videoId: String,
        limit: Int? = null,
        offset: Int? = null,
    ): Flow<Resource<List<TranslatedLyricResponseDTO>>> =
        translatedLyricRepository
            .findByVideoId(videoId, limit, offset)
            .mapSuccess { translatedLyrics -> translatedLyrics.map { it.toResponseDTO() } }
            .catchToResourceError()

    fun getTranslatedLyricsByLanguage(
        language: String,
        limit: Int? = null,
        offset: Int? = null,
    ): Flow<Resource<List<TranslatedLyricResponseDTO>>> =
        translatedLyricRepository
            .findByLanguage(language, limit, offset)
            .mapSuccess { translatedLyrics -> translatedLyrics.map { it.toResponseDTO() } }
            .catchToResourceError()

    fun getTranslatedLyricByVideoIdAndLanguage(
        videoId: String,
        language: String,
    ): Flow<Resource<TranslatedLyricResponseDTO?>> =
        flow {
            emit(Resource.Loading)

            try {
                // First, try to find translated lyric
                val translatedLyricResult = translatedLyricRepository.findByVideoIdAndLanguage(videoId, language).last()

                when (translatedLyricResult) {
                    is Resource.Success if (translatedLyricResult.data != null) -> {
                        // Translated lyric found, return it
                        val translatedLyricDTO = translatedLyricResult.data.toResponseDTO()
                        emit(Resource.Success(translatedLyricDTO))
                    }
                    is Resource.Loading -> {
                    }
                    else -> {
                        // No translated lyric found, check if already in notfound_translated_lyrics
                        logger.debug("getTranslatedLyricByVideoIdAndLanguage --> No translated lyric found for videoId: $videoId, language: $language, checking notfound_translated_lyrics...")

                        val sha256hash = (videoId + language).sha256()
                        val notFoundResult = notFoundTranslatedLyricRepository.findBySha256Hash(sha256hash).last()
                        when (notFoundResult) {
                            is Resource.Success if (notFoundResult.data != null) -> {
                                logger.debug("getTranslatedLyricByVideoIdAndLanguage --> VideoId: $videoId, language: $language already exists in notfound_translated_lyrics")
                            }
                            is Resource.Loading -> {
                            }
                            else -> {
                                logger.info("getTranslatedLyricByVideoIdAndLanguage --> Adding videoId: $videoId, language: $language to notfound_translated_lyrics")
                                val notFoundTranslatedLyric =
                                    NotFoundTranslatedLyric(
                                        videoId = videoId,
                                        translationLanguage = language,
                                        addedDate = LocalDateTime.now(),
                                        sha256hash = sha256hash,
                                    )

                                // Save to notfound_translated_lyrics
                                val saveResult = notFoundTranslatedLyricRepository.save(notFoundTranslatedLyric).last()
                                when (saveResult) {
                                    is Resource.Success -> {
                                        logger.info("getTranslatedLyricByVideoIdAndLanguage --> Successfully saved NotFoundTranslatedLyric for videoId: $videoId, language: $language")
                                    }
                                    is Resource.Error -> {
                                        logger.warn("getTranslatedLyricByVideoIdAndLanguage --> Failed to save NotFoundTranslatedLyric for videoId: $videoId, language: $language - ${saveResult.message}")
                                    }
                                    else -> {} // Loading state
                                }
                            } // Loading state
                        }
                    }
                }
            } catch (e: Exception) {
                emit(Resource.Error("Error getting translated lyric by videoId and language", e))
            }
        }.flowOn(Dispatchers.IO)

    fun saveTranslatedLyric(translatedLyricRequestDTO: TranslatedLyricRequestDTO): Flow<Resource<TranslatedLyricResponseDTO>> =
        flow {
            emit(Resource.Loading)

            try {
                val translatedLyric = translatedLyricRequestDTO.toEntity()

                // Check for duplicate data based on sha256hash
                logger.debug(
                    "saveTranslatedLyric --> Checking for duplicate translated lyric with sha256hash: ${translatedLyric.sha256hash}",
                )
                val existingResult = translatedLyricRepository.findBySha256Hash(translatedLyric.sha256hash).last()

                when (existingResult) {
                    is Resource.Success -> {
                        if (existingResult.data != null) {
                            // Translated lyric with this sha256hash already exists
                            logger.warn(
                                "saveTranslatedLyric --> Duplicate translated lyric detected with sha256hash: ${translatedLyric.sha256hash}",
                            )
                            emit(Resource.duplicateError("This translated lyrics already exists"))
                            return@flow
                        }
                    }
                    is Resource.Error -> {
                        logger.warn("saveTranslatedLyric --> Error checking for duplicate translated lyric: ${existingResult.message}")
                        // Continue to save if unable to check for duplicates
                    }
                    else -> {} // Loading state
                }

                val saveResult = translatedLyricRepository.save(translatedLyric).last()

                when (saveResult) {
                    is Resource.Success -> {
                        // When translated lyric is successfully saved, remove from notfound_translated_lyrics if exists
                        logger.debug("saveTranslatedLyric --> Translated lyric saved successfully, checking if videoId: ${translatedLyricRequestDTO.videoId}, language: ${translatedLyricRequestDTO.language} exists in notfound_translated_lyrics")

                        try {
                            val deleteResult = notFoundTranslatedLyricRepository.deleteByVideoIdAndLanguage(translatedLyricRequestDTO.videoId, translatedLyricRequestDTO.language).last()
                            when (deleteResult) {
                                is Resource.Success -> {
                                    if (deleteResult.data) {
                                        logger.info("saveTranslatedLyric --> Removed videoId: ${translatedLyricRequestDTO.videoId}, language: ${translatedLyricRequestDTO.language} from notfound_translated_lyrics after adding translated lyric")
                                    }
                                }
                                is Resource.Error -> {
                                    logger.warn(
                                        "saveTranslatedLyric --> Failed to remove videoId: ${translatedLyricRequestDTO.videoId}, language: ${translatedLyricRequestDTO.language} from notfound_translated_lyrics: ${deleteResult.message}",
                                    )
                                }
                                else -> {} // Loading state
                            }
                        } catch (e: Exception) {
                            logger.warn("saveTranslatedLyric --> Exception while removing videoId: ${translatedLyricRequestDTO.videoId}, language: ${translatedLyricRequestDTO.language} from notfound_translated_lyrics", e)
                        }

                        emit(Resource.Success(saveResult.data.toResponseDTO()))
                    }
                    is Resource.Error -> {
                        emit(Resource.Error(saveResult.message, saveResult.exception))
                    }
                    else -> {} // Loading state
                }
            } catch (e: Exception) {
                emit(Resource.Error("Error saving translated lyric", e))
            }
        }.flowOn(Dispatchers.IO)

    /**
     * Vote for a translated lyric (upvote or downvote)
     * @param voteDTO The vote data transfer object containing the translated lyric ID and vote value
     * @return A flow of Resource<TranslatedLyricResponseDTO> representing the updated translated lyric
     */
    fun voteTranslatedLyric(voteDTO: VoteRequestDTO): Flow<Resource<TranslatedLyricResponseDTO>> =
        flow {
            emit(Resource.Loading)
            logger.debug("voteTranslatedLyric --> Processing vote for translated lyric id: ${voteDTO.id}")

            try {
                // Convert vote value to increment: 1 for upvote, -1 for downvote
                val voteIncrement = if (voteDTO.vote == 1) 1 else -1
                logger.debug("voteTranslatedLyric --> Vote increment: $voteIncrement")

                val result = translatedLyricRepository.updateVote(voteDTO.id, voteIncrement).last()

                when (result) {
                    is Resource.Success -> {
                        logger.debug("voteTranslatedLyric --> Successfully updated vote for translated lyric id: ${voteDTO.id}")
                        emit(Resource.Success(result.data.toResponseDTO()))
                    }
                    is Resource.Error -> {
                        logger.error(
                            "voteTranslatedLyric --> Failed to update vote for translated lyric id: ${voteDTO.id}: ${result.message}",
                        )
                        emit(Resource.Error(result.message, result.exception, result.code))
                    }
                    else -> {} // Loading state
                }
            } catch (e: Exception) {
                logger.error("voteTranslatedLyric --> Error processing vote: ${e.message}", e)
                emit(Resource.Error("Failed to process vote: ${e.message}", e))
            }
        }.flowOn(Dispatchers.IO)

    fun getAllNotFoundLyrics(
        limit: Int? = null,
        offset: Int? = null,
    ): Flow<Resource<List<NotFoundLyricResponseDTO>>> =
        notFoundLyricRepository
            .findAllOrderedByDate(limit, offset)
            .mapSuccess { notFoundLyrics -> notFoundLyrics.map { it.toResponseDTO() } }
            .catchToResourceError()

    fun getAllNotFoundTranslatedLyrics(
        limit: Int? = null,
        offset: Int? = null,
    ): Flow<Resource<List<NotFoundTranslatedLyricResponseDTO>>> =
        notFoundTranslatedLyricRepository
            .findAllOrderedByDate(limit, offset)
            .mapSuccess { notFoundTranslatedLyrics -> notFoundTranslatedLyrics.map { it.toResponseDTO() } }
            .catchToResourceError()
}
