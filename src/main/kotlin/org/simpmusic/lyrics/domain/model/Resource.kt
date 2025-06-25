package org.simpmusic.lyrics.domain.model

/**
 * Resource class for handling data states (Success, Error, Loading)
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Exception? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()

    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        fun error(message: String, exception: Exception? = null): Resource<Nothing> = Error(message, exception)
        fun loading(): Resource<Nothing> = Loading
    }
} 