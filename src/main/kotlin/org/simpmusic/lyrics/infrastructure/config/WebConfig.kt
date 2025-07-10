package org.simpmusic.lyrics.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry

/**
 * Application configuration for web components
 */
@Configuration
class WebConfig(private val hmacInterceptor: HmacInterceptor) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT")
            .maxAge(3600)
    }
    
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(hmacInterceptor)
            .addPathPatterns("/api/lyrics/**")
    }
}
