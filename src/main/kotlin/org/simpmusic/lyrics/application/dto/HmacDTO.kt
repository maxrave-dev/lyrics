package org.simpmusic.lyrics.application.dto

data class HmacDTO(
    val uri: String,
    val timestamp: String,
    val hmac: String
)