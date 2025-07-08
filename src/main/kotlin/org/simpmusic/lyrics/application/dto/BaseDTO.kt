package org.simpmusic.lyrics.application.dto

interface BaseDTO {
    /**
     * Hash the main data of the DTO to create a unique identifier.
     * Use SHA256 to generate 64 character unique ID.
     */
    fun getUniqueId(): String
}