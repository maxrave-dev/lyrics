package org.simpmusic.lyrics.infrastructure.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.simpmusic.lyrics.application.service.LyricService
import org.simpmusic.lyrics.domain.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * Appwrite initialization on application startup
 */
@Configuration
class AppwriteInitializer(
    private val lyricService: LyricService,
    private val environment: Environment,
    @Qualifier("applicationScope") private val applicationScope: CoroutineScope,
) {
    private val logger = LoggerFactory.getLogger(AppwriteInitializer::class.java)

    @Bean
    fun initAppwrite(): CommandLineRunner =
        CommandLineRunner {
            // Only initialize in certain profiles, not in test environment
            val activeProfiles = environment.activeProfiles
            val shouldInitialize =
                activeProfiles.isEmpty() ||
                    !listOf("test").any { it in activeProfiles }

            if (shouldInitialize) {
                logger.info("Initializing Appwrite database and collections...")
                try {
                    lyricService
                        .initializeAppwrite()
                        .onStart { logger.info("Starting Appwrite initialization...") }
                        .onEach { result ->
                            when (result) {
                                is Resource.Success -> {
                                    logger.info("Appwrite initialization completed successfully: ${result.data}")
                                }

                                is Resource.Error -> {
                                    logger.error("Failed to initialize Appwrite: ${result.message}", result.exception)
                                }
                            }
                        }.onCompletion { error ->
                            if (error != null) {
                                logger.error("Appwrite initialization failed with exception", error)
                            } else {
                                logger.info("Appwrite initialization flow completed")
                            }
                        }.catch { e ->
                            logger.error("Exception during Appwrite initialization flow", e)
                        }.launchIn(applicationScope)
                } catch (e: Exception) {
                    logger.error("Failed to initialize Appwrite: ${e.message}", e)
                }
            } else {
                logger.info("Skipping Appwrite initialization in test environment")
            }
        }
}
