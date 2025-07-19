package org.simpmusic.lyrics.application.dto.response

import org.springframework.http.HttpStatus

/**
 * Data class đại diện cho thông tin trạng thái đang xử lý.
 * Được sử dụng trong ApiResult.Loading để cung cấp thông tin về yêu cầu đang được xử lý.
 *
 * @property code Mã trạng thái HTTP
 * @property message Thông báo mô tả trạng thái xử lý
 */
data class LoadingResponseDTO(
    val code: Int,
    val message: String,
) : BaseResponseDTO {
    companion object {
        /**
         * Tạo một LoadingResponseDTO từ mã và thông báo
         *
         * @param code Mã HTTP (mặc định: 102 PROCESSING)
         * @param message Thông báo mô tả
         * @return LoadingResponseDTO
         */
        fun fromMessage(code: Int = HttpStatus.PROCESSING.value(), message: String): LoadingResponseDTO {
            return LoadingResponseDTO(
                code = code,
                message = message,
            )
        }

        /**
         * Tạo một LoadingResponseDTO cho trường hợp đang xử lý yêu cầu chung
         *
         * @return LoadingResponseDTO với thông báo mặc định
         */
        fun processing(): LoadingResponseDTO {
            return LoadingResponseDTO(
                code = HttpStatus.PROCESSING.value(),
                message = "Request is being processed",
            )
        }
    }
}