package org.simpmusic.lyrics.infrastructure.config

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.util.concurrent.TimeUnit
import javax.cache.CacheManager
import javax.cache.Caching
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.CreatedExpiryPolicy
import javax.cache.expiry.Duration

/**
 * Configuration class for JCache with Caffeine provider
 */
@Configuration
class CacheConfig {
    
    @Bean
    @Primary
    fun cacheManager(): CacheManager {
        val provider = Caching.getCachingProvider(CaffeineCachingProvider::class.java.name)
        val cacheManager = provider.cacheManager
        
        val bucketCacheConfig = MutableConfiguration<Any, Any>()
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration(TimeUnit.HOURS, 1)))
            .setStoreByValue(false)
            .setStatisticsEnabled(true)
        
        if (cacheManager.getCache<Any, Any>("buckets") == null) {
            cacheManager.createCache("buckets", bucketCacheConfig)
        }
        
        if (cacheManager.getCache<Any, Any>("filterConfigCache") == null) {
            cacheManager.createCache("filterConfigCache", bucketCacheConfig)
        }
        
        return cacheManager
    }
} 