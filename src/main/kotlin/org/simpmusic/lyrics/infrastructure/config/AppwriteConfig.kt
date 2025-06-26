package org.simpmusic.lyrics.infrastructure.config

import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppwriteConfig {
    
    @Value("\${appwrite.endpoint}")
    private lateinit var endpoint: String
    
    @Value("\${appwrite.project}")
    private lateinit var projectId: String
    
    @Value("\${appwrite.database}")
    private lateinit var databaseId: String
    
    @Value("\${appwrite.collection.lyrics}")
    private lateinit var lyricsCollectionId: String

    @Value("\${appwrite.collection.translated_lyrics}")
    private lateinit var translatedLyricsCollectionId: String

    @Value("\${appwrite.collection.notfound_lyrics}")
    private lateinit var notFoundLyricsCollectionId: String
    
    @Value("\${appwrite.apikey}")
    private lateinit var apiKey: String
    
    @Bean
    fun appwriteClient(): Client {
        return Client()
            .setEndpoint(endpoint)
            .setProject(projectId)
            .setKey(apiKey)
    }
    
    @Bean
    fun appwriteDatabases(client: Client): Databases {
        return Databases(client)
    }
    
    @Bean
    fun appwriteAccount(client: Client): Account {
        return Account(client)
    }
    
    @Bean
    fun databaseId(): String {
        return databaseId
    }
    
    @Bean
    fun lyricsCollectionId(): String {
        return lyricsCollectionId
    }

    @Bean
    fun translatedLyricsCollectionId(): String {
        return translatedLyricsCollectionId
    }

    @Bean
    fun notFoundLyricsCollectionId(): String {
        return notFoundLyricsCollectionId
    }
} 