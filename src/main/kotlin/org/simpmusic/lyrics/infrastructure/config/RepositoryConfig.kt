package org.simpmusic.lyrics.infrastructure.config

import io.appwrite.services.Databases
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.domain.repository.NotFoundLyricRepository
import org.simpmusic.lyrics.domain.repository.NotFoundTranslatedLyricRepository
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.simpmusic.lyrics.infrastructure.persistence.AppwriteLyricRepository
import org.simpmusic.lyrics.infrastructure.persistence.AppwriteNotFoundLyricRepository
import org.simpmusic.lyrics.infrastructure.persistence.AppwriteNotFoundTranslatedLyricRepository
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

    @Bean
    @Primary
    fun notFoundLyricRepository(
        databases: Databases,
        databaseId: String,
        notFoundLyricsCollectionId: String,
    ): NotFoundLyricRepository = AppwriteNotFoundLyricRepository(databases, databaseId, notFoundLyricsCollectionId)

    @Bean
    @Primary
    fun notFoundTranslatedLyricRepository(
        databases: Databases,
        databaseId: String,
        notFoundTranslatedLyricsCollectionId: String,
    ): NotFoundTranslatedLyricRepository =
        AppwriteNotFoundTranslatedLyricRepository(databases, databaseId, notFoundTranslatedLyricsCollectionId)
} 
