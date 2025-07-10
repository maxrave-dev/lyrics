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
        // Lấy JCache provider từ Caffeine
        val provider = Caching.getCachingProvider(CaffeineCachingProvider::class.java.name)
        val cacheManager = provider.cacheManager
        
        // Cấu hình cache cho Bucket4j
        val bucketCacheConfig = MutableConfiguration<Any, Any>()
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration(TimeUnit.HOURS, 1)))
            .setStoreByValue(false)
            .setStatisticsEnabled(true)
        
        // Tạo cache nếu chưa tồn tại
        if (cacheManager.getCache<Any, Any>("buckets") == null) {
            cacheManager.createCache("buckets", bucketCacheConfig)
        }
        
        // Tạo cache cho cấu hình filter nếu chưa tồn tại
        if (cacheManager.getCache<Any, Any>("filterConfigCache") == null) {
            cacheManager.createCache("filterConfigCache", bucketCacheConfig)
        }
        
        return cacheManager
    }
} 