package org.simpmusic.lyrics.infrastructure.config

import org.simpmusic.lyrics.application.service.LyricService
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.simpmusic.lyrics.infrastructure.datasource.AppwriteDataSource
import org.simpmusic.lyrics.infrastructure.datasource.MeilisearchDataSource
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
        appwriteDataSource: AppwriteDataSource,
        meilisearchDataSource: MeilisearchDataSource,
    ): LyricService =
        LyricService(
            lyricRepository,
            translatedLyricRepository,
            appwriteDataSource,
            meilisearchDataSource,
        )
}
