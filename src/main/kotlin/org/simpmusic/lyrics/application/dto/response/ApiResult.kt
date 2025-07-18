package org.simpmusic.lyrics.application.dto.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Sealed class để đại diện cho tất cả các loại phản hồi API có thể xảy ra.
 * Điều này cho phép xử lý phản hồi API một cách an toàn về kiểu (type-safe).
 *
 * @param T Kiểu dữ liệu thành công
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class ApiResult<out T> {
    /**
     * Đại diện cho phản hồi API thành công với dữ liệu.
     *
     * @param data Dữ liệu phản hồi
     */
    data class Success<T>(
        val data: T,
        val success: Boolean = true,
    ) : ApiResult<T>()

    /**
     * Đại diện cho phản hồi API lỗi.
     *
     * @param error Thông tin chi tiết về lỗi
     */
    data class Error(
        val error: ErrorResponseDTO,
        val success: Boolean = false,
    ) : ApiResult<Nothing>()

    /**
     * Đại diện cho phản hồi API đang xử lý.
     *
     * @param processing Thông tin về trạng thái xử lý
     */
    data class Loading<T>(
        val processing: LoadingResponseDTO,
        val success: Boolean = false,
    ) : ApiResult<T>()

    companion object {
        /**
         * Tạo phản hồi thành công với dữ liệu
         */
        fun <T> success(data: T): ApiResult<T> = Success(data)

        /**
         * Tạo phản hồi lỗi từ ErrorResponseDTO
         */
        fun error(errorResponseDTO: ErrorResponseDTO): ApiResult<Nothing> = Error(errorResponseDTO)

        /**
         * Tạo phản hồi lỗi từ mã lỗi và thông báo
         */
        fun error(code: Int, reason: String): ApiResult<Nothing> =
            Error(
                ErrorResponseDTO(
                    code = code,
                    reason = reason,
                ),
            )

        /**
         * Tạo phản hồi Not Found với thông báo
         */
        fun notFound(reason: String = "Resource not found"): ApiResult<Nothing> =
            Error(ErrorResponseDTO.notFound(reason))

        /**
         * Tạo phản hồi Server Error với thông báo
         */
        fun serverError(reason: String = "Internal server error"): ApiResult<Nothing> =
            Error(ErrorResponseDTO.serverError(reason))

        /**
         * Tạo phản hồi đang xử lý với thông báo
         */
        fun <T> loading(code: Int, message: String): ApiResult<T> =
            Loading(LoadingResponseDTO.fromMessage(code, message))
    }
}