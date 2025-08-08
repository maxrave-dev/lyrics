package org.simpmusic.lyrics.infrastructure.config

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MeilisearchConfig {

    @Value("\${meilisearch.host:http://localhost:7700}")
    private lateinit var host: String

    @Value("\${meilisearch.masterKey:}")
    private lateinit var masterKey: String

    @Bean
    fun meilisearchClient(): Client {
        val config = Config(host, masterKey.takeIf { it.isNotBlank() })
        return Client(config)
    }
}