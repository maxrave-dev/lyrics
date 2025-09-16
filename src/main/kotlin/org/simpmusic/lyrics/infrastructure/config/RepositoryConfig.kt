package org.simpmusic.lyrics.infrastructure.config

import io.appwrite.services.Databases
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.simpmusic.lyrics.infrastructure.persistence.AppwriteLyricRepository
import org.simpmusic.lyrics.infrastructure.persistence.AppwriteTranslatedLyricRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Application configuration for repository components
 */
@Configuration
class RepositoryConfig {
    @Bean
    @Primary
    fun lyricRepository(
        databases: Databases,
        databaseId: String,
        lyricsCollectionId: String,
    ): LyricRepository = AppwriteLyricRepository(databases, databaseId, lyricsCollectionId)

    @Bean
    @Primary
    fun translatedLyricRepository(
        databases: Databases,
        databaseId: String,
        translatedLyricsCollectionId: String,
    ): TranslatedLyricRepository = AppwriteTranslatedLyricRepository(databases, databaseId, translatedLyricsCollectionId)
}
