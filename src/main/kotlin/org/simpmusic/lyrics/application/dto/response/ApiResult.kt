package org.simpmusic.lyrics.application.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Objects
import kotlin.reflect.KClass

/**
 * Sealed class to represent all possible API response types.
 * This allows type-safe handling of API responses.
 *
 * @param T The success data type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ApiResult.Success::class, name = "success"),
    JsonSubTypes.Type(value = ApiResult.Error::class, name = "error"),
)
@Schema(
    description = "API Result wrapper",
    discriminatorProperty = "type",
    discriminatorMapping = [
        DiscriminatorMapping(value = "success", schema = ApiResult.Success::class),
        DiscriminatorMapping(value = "error", schema = ApiResult.Error::class),
    ],
    oneOf = [ApiResult.Success::class, ApiResult.Error::class],
)
sealed class ApiResult<out T> {
    /**
     * Represents a successful API response with data.
     *
     * @param data The response data
     */
    data class Success<T>(
        val data: T,
        val success: Boolean = true,
    ) : ApiResult<T>()

    /**
     * Represents an error API response.
     *
     * @param error Detailed error information
     */
    data class Error(
        val error: ErrorResponseDTO,
        val success: Boolean = false,
    ) : ApiResult<Nothing>()

    companion object {
        /**
         * Create a successful response with data
         */
        fun <T> success(data: T): ApiResult<T> = Success(data)

        /**
         * Create an error response from ErrorResponseDTO
         */
        fun error(errorResponseDTO: ErrorResponseDTO): ApiResult<Nothing> = Error(errorResponseDTO)

        /**
         * Create an error response from error code and message
         */
        fun error(
            code: Int,
            reason: String,
        ): ApiResult<Nothing> =
            Error(
                ErrorResponseDTO(
                    code = code,
                    reason = reason,
                ),
            )

        /**
         * Create a Not Found response with message
         */
        fun notFound(reason: String = "Resource not found"): ApiResult<Nothing> = Error(ErrorResponseDTO.notFound(reason))

        /**
         * Create a Server Error response with message
         */
        fun serverError(reason: String = "Internal server error"): ApiResult<Nothing> = Error(ErrorResponseDTO.serverError(reason))
    }
}
