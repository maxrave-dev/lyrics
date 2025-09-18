package org.simpmusic.lyrics.infrastructure.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.simpmusic.lyrics.application.service.LyricService
import org.simpmusic.lyrics.domain.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * MongoDB initialization on application startup
 */
@Configuration
class MongoInitializer(
    private val lyricService: LyricService,
    private val environment: Environment,
    @param:Qualifier("applicationScope") private val applicationScope: CoroutineScope,
) {
    private val logger = LoggerFactory.getLogger(MongoInitializer::class.java)

    @Bean
    fun initMongoDB(): CommandLineRunner =
        CommandLineRunner {
            // Only initialize in certain profiles, not in test environment
            val activeProfiles = environment.activeProfiles
            val shouldInitialize =
                activeProfiles.isEmpty() ||
                    !listOf("test").any { it in activeProfiles }

            if (shouldInitialize) {
                logger.info("Checking MongoDB connection and initializing services...")
                try {
                    lyricService
                        .initializeServices()
                        .onStart { logger.info("Starting services initialization...") }
                        .onEach { result ->
                            when (result) {
                                is Resource.Success -> {
                                    logger.info("Services initialization completed successfully: ${result.data}")
                                }

                                is Resource.Error -> {
                                    logger.error("Failed to initialize services: ${result.message}", result.exception)
                                }
                            }
                        }.onCompletion { error ->
                            if (error != null) {
                                logger.error("Services initialization failed with exception", error)
                            } else {
                                logger.info("Services initialization flow completed")
                            }
                        }.catch { e ->
                            logger.error("Exception during services initialization flow", e)
                        }.launchIn(applicationScope)
                } catch (e: Exception) {
                    logger.error("Failed to initialize services: ${e.message}", e)
                }
            } else {
                logger.info("Skipping MongoDB initialization in test environment")
            }
        }
}
