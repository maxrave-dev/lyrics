package org.simpmusic.lyrics.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Application configuration for web components
 */
@Configuration
class WebConfig(
    private val hmacInterceptor: HmacInterceptor,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/v1/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT")
            .maxAge(3600)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(hmacInterceptor)
            .addPathPatterns("/v1/**")
    }
}
