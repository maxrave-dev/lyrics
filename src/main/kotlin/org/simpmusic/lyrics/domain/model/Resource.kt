package org.simpmusic.lyrics.domain.model

import org.simpmusic.lyrics.application.dto.response.ErrorResponseDTO

/**
 * Resource class for handling data states (Success, Error, Loading)
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(
        val message: String, 
        val exception: Exception? = null,
        val code: Int? = null
    ) : Resource<Nothing>() {
        fun toErrorResponse(): ErrorResponseDTO {
            return ErrorResponseDTO(
                error = true,
                code = code ?: ErrorResponseDTO.INTERNAL_SERVER_ERROR,
                reason = message
            )
        }
    }
    object Loading : Resource<Nothing>()

    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        
        fun error(message: String, exception: Exception? = null): Resource<Nothing> = 
            Error(message, exception)
            
        fun error(message: String, code: Int, exception: Exception? = null): Resource<Nothing> = 
            Error(message, exception, code)
            
        fun loading(): Resource<Nothing> = Loading
        
        // Convenience methods for common error types
        fun duplicateError(message: String = "Data already exists"): Resource<Nothing> = 
            Error(message, null, ErrorResponseDTO.CONFLICT)
            
        fun badRequestError(message: String = "Invalid request"): Resource<Nothing> = 
            Error(message, null, ErrorResponseDTO.BAD_REQUEST)
            
        fun notFoundError(message: String = "Resource not found"): Resource<Nothing> = 
            Error(message, null, ErrorResponseDTO.NOT_FOUND)
            
        fun serverError(message: String = "Internal server error"): Resource<Nothing> = 
            Error(message, null, ErrorResponseDTO.INTERNAL_SERVER_ERROR)
    }
} 