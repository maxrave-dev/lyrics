package org.simpmusic.lyrics.infrastructure.config

import kotlinx.coroutines.CoroutineScope
import org.simpmusic.lyrics.application.service.LyricService
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.simpmusic.lyrics.domain.repository.NotFoundLyricRepository
import org.simpmusic.lyrics.infrastructure.datasource.AppwriteDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Application configuration for service components
 */
@Configuration
class ApplicationConfig {

    @Bean
    fun lyricService(
        lyricRepository: LyricRepository,
        translatedLyricRepository: TranslatedLyricRepository,
        notFoundLyricRepository: NotFoundLyricRepository,
        appwriteDataSource: AppwriteDataSource,
        @Qualifier("serviceScope") serviceScope: CoroutineScope
    ): LyricService {
        return LyricService(lyricRepository, translatedLyricRepository, notFoundLyricRepository, appwriteDataSource, serviceScope)
    }
} 