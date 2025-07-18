package org.simpmusic.lyrics.application.dto.response

import org.simpmusic.lyrics.application.dto.response.BaseResponseDTO

/**
 * Standard error response format for API
 *
 * Example response format:
 * ```json
 * {
 *   "error": true,
 *   "code": 409,
 *   "reason": "This lyrics already exists"
 * }
 * ```
 *
 * Common usage scenarios:
 * - 409 (CONFLICT): When trying to create duplicate data
 * - 400 (BAD_REQUEST): Invalid input or parameters
 * - 404 (NOT_FOUND): Resource not found
 * - 422 (UNPROCESSABLE_ENTITY): Validation failed
 * - 500 (INTERNAL_SERVER_ERROR): Server error
 */
data class ErrorResponseDTO(
    val error: Boolean = true,
    val code: Int,
    val reason: String,
) : BaseResponseDTO {
    companion object {
        // HTTP Status codes for common error scenarios
        const val CONFLICT = 409 // Duplicate data
        const val BAD_REQUEST = 400 // Invalid input
        const val NOT_FOUND = 404 // Resource not found
        const val INTERNAL_SERVER_ERROR = 500 // Server error
        const val UNPROCESSABLE_ENTITY = 422 // Validation error

        /**
         * Create error response for duplicate data
         * Usage: ErrorResponseDTO.duplicateData("This lyrics already exists")
         * Returns: { "error": true, "code": 409, "reason": "This lyrics already exists" }
         */
        fun duplicateData(reason: String = "Data already exists") =
            ErrorResponseDTO(
                code = CONFLICT,
                reason = reason,
            )

        /**
         * Create error response for bad request
         * Usage: ErrorResponseDTO.badRequest("Invalid videoId format")
         * Returns: { "error": true, "code": 400, "reason": "Invalid videoId format" }
         */
        fun badRequest(reason: String = "Invalid request") =
            ErrorResponseDTO(
                code = BAD_REQUEST,
                reason = reason,
            )

        /**
         * Create error response for not found
         * Usage: ErrorResponseDTO.notFound("Lyrics not found")
         * Returns: { "error": true, "code": 404, "reason": "Lyrics not found" }
         */
        fun notFound(reason: String = "Resource not found") =
            ErrorResponseDTO(
                code = NOT_FOUND,
                reason = reason,
            )

        /**
         * Create error response for server error
         * Usage: ErrorResponseDTO.serverError("Database connection failed")
         * Returns: { "error": true, "code": 500, "reason": "Database connection failed" }
         */
        fun serverError(reason: String = "Internal server error") =
            ErrorResponseDTO(
                code = INTERNAL_SERVER_ERROR,
                reason = reason,
            )

        /**
         * Create error response for validation error
         * Usage: ErrorResponseDTO.validationError("Required field missing: songTitle")
         * Returns: { "error": true, "code": 422, "reason": "Required field missing: songTitle" }
         */
        fun validationError(reason: String = "Validation failed") =
            ErrorResponseDTO(
                code = UNPROCESSABLE_ENTITY,
                reason = reason,
            )
    }
}
