package org.simpmusic.lyrics.infrastructure.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.simpmusic.lyrics.application.dto.response.ErrorResponseDTO
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Interceptor to validate HMAC tokens for non-GET requests
 */
@Component
class HmacInterceptor(
    private val hmacService: HmacService,
    private val objectMapper: ObjectMapper
) : HandlerInterceptor {
    
    private val logger = LoggerFactory.getLogger(HmacInterceptor::class.java)
    private val nonGetMethods = setOf(
        HttpMethod.POST.name(), 
        HttpMethod.PUT.name(), 
        HttpMethod.DELETE.name(), 
        HttpMethod.PATCH.name()
    )
    
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // Only apply to non-GET requests
        if (request.method !in nonGetMethods) {
            return true
        }
        
        logger.debug("HmacInterceptor --> Validating HMAC token for ${request.method} ${request.requestURI}")
        
        val timestamp = request.getHeader("X-Timestamp")
        val hmac = request.getHeader("X-HMAC")
        
        if (timestamp == null || hmac == null) {
            logger.warn("HmacInterceptor --> Missing required headers: X-Timestamp or X-HMAC for ${request.method} ${request.requestURI}")
            sendErrorResponse(response, ErrorResponseDTO.badRequest("Missing X-Timestamp or X-HMAC header"))
            return false
        }
        
        // Check if timestamp is valid
        if (!hmacService.isValidTimestamp(timestamp)) {
            logger.warn("HmacInterceptor --> Invalid timestamp: $timestamp for ${request.method} ${request.requestURI}")
            sendErrorResponse(response, ErrorResponseDTO.badRequest("Invalid or expired timestamp"))
            return false
        }
        
        // Data to create HMAC: [timestamp + requestURI]
        val data = "$timestamp${request.requestURI}"
        
        // Check if HMAC is valid
        if (!hmacService.validateHmac(data, hmac)) {
            logger.warn("HmacInterceptor --> Invalid HMAC token for ${request.method} ${request.requestURI}")
            sendErrorResponse(response, ErrorResponseDTO.badRequest("Invalid HMAC token"))
            return false
        }
        
        logger.debug("HmacInterceptor --> Valid HMAC token for ${request.method} ${request.requestURI}")
        return true
    }
    
    private fun sendErrorResponse(response: HttpServletResponse, errorResponse: ErrorResponseDTO) {
        response.status = errorResponse.code
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
} 