package org.simpmusic.lyrics.infrastructure.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.simpmusic.lyrics.infrastructure.datasource.MeilisearchDataSource
import org.simpmusic.lyrics.domain.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * Meilisearch initialization on application startup
 */
@Configuration
class MeilisearchInitializer(
    private val meilisearchDataSource: MeilisearchDataSource,
    private val environment: Environment,
    @Qualifier("applicationScope") private val applicationScope: CoroutineScope,
) {
    private val logger = LoggerFactory.getLogger(MeilisearchInitializer::class.java)

    @Bean
    fun initMeilisearch(): CommandLineRunner =
        CommandLineRunner {
            // Only initialize in certain profiles, not in test environment
            val activeProfiles = environment.activeProfiles
            val shouldInitialize =
                activeProfiles.isEmpty() ||
                    !listOf("test").any { it in activeProfiles }

            if (shouldInitialize) {
                logger.info("Initializing Meilisearch index and settings...")
                try {
                    meilisearchDataSource
                        .initializeMeilisearch()
                        .onStart { logger.info("Starting Meilisearch initialization...") }
                        .onEach { result ->
                            when (result) {
                                is Resource.Success -> {
                                    logger.info("Meilisearch initialization completed successfully: ${result.data}")
                                }

                                is Resource.Error -> {
                                    logger.error("Failed to initialize Meilisearch: ${result.message}", result.exception)
                                }
                                
                                else -> {} // Loading state
                            }
                        }.onCompletion { error ->
                            if (error != null) {
                                logger.error("Meilisearch initialization failed with exception", error)
                            } else {
                                logger.info("Meilisearch initialization flow completed")
                            }
                        }.catch { e ->
                            logger.error("Exception during Meilisearch initialization flow", e)
                        }.launchIn(applicationScope)
                } catch (e: Exception) {
                    logger.error("Failed to initialize Meilisearch: ${e.message}", e)
                }
            } else {
                logger.info("Skipping Meilisearch initialization in test environment")
            }
        }
}