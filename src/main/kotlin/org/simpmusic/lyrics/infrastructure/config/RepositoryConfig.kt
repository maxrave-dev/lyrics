package org.simpmusic.lyrics.infrastructure.config

import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.domain.repository.TranslatedLyricRepository
import org.simpmusic.lyrics.infrastructure.persistence.MongoLyricRepository
import org.simpmusic.lyrics.infrastructure.persistence.MongoTranslatedLyricRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.MongoTemplate

/**
 * Application configuration for repository components
 */
@Configuration
class RepositoryConfig {
    @Bean
    @Primary
    fun lyricRepository(mongoTemplate: MongoTemplate): LyricRepository = MongoLyricRepository(mongoTemplate)

    @Bean
    @Primary
    fun translatedLyricRepository(mongoTemplate: MongoTemplate): TranslatedLyricRepository =
        MongoTranslatedLyricRepository(mongoTemplate)
}
