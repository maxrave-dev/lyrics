package org.simpmusic.lyrics

import io.appwrite.services.Databases
import org.mockito.Mockito
import org.simpmusic.lyrics.domain.repository.LyricRepository
import org.simpmusic.lyrics.infrastructure.datasource.AppwriteDataSource
import org.simpmusic.lyrics.infrastructure.persistence.InMemoryLyricRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")
class TestConfig {
    
    @Bean
    @Primary
    fun mockLyricRepository(): LyricRepository {
        return InMemoryLyricRepository()
    }
    
    @Bean
    @Primary
    fun mockDatabases(): Databases {
        return Mockito.mock(Databases::class.java)
    }
    
    @Bean
    @Primary
    fun mockAppwriteDataSource(): AppwriteDataSource {
        return Mockito.mock(AppwriteDataSource::class.java)
    }
    
    @Bean
    @Primary
    fun databaseId(): String {
        return "test_database_id"
    }
    
    @Bean
    @Primary
    fun lyricsCollectionId(): String {
        return "test_lyrics_collection_id"
    }
} 