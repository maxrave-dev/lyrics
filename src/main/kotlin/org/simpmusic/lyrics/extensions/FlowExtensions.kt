package org.simpmusic.lyrics.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.simpmusic.lyrics.domain.model.Resource
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("FlowExtensions")

/**
 * Logs each emission from a flow with a custom prefix
 */
fun <T> Flow<T>.logEach(prefix: String): Flow<T> = this.onEach {
    logger.info("$prefix: $it")
}

/**
 * Logs errors from a flow with a custom prefix
 */
fun <T> Flow<T>.logError(prefix: String): Flow<T> = this.catch { e ->
    logger.error("$prefix error: ${e.message}", e)
    throw e
}

/**
 * Logs the completion of a flow with a custom message
 */
fun <T> Flow<T>.logCompletion(message: String): Flow<T> = this.onCompletion {
    logger.info("Flow completed: $message")
}

/**
 * Converts a flow to a hot flow and shares it in the given scope
 */
fun <T> Flow<T>.shareHot(scope: CoroutineScope, replay: Int = 1): Flow<T> = 
    this.shareIn(scope, SharingStarted.Eagerly, replay)

/**
 * Utility extension for handling Resource flows
 * Applies the given transformations to the data in Success state
 */
fun <T, R> Flow<Resource<T>>.mapSuccess(transform: (T) -> R): Flow<Resource<R>> = 
    this.map { resource ->
        when (resource) {
            is Resource.Success -> Resource.Success(transform(resource.data))
            is Resource.Error -> Resource.Error(resource.message, resource.exception)
            is Resource.Loading -> Resource.Loading
        }
    }

/**
 * Utility extension for handling Resource flows
 * Applies the given transformations to the data in Success state if it's not null
 */
fun <T, R> Flow<Resource<T?>>.mapSuccessNotNull(transform: (T) -> R): Flow<Resource<R?>> = 
    this.map { resource ->
        when (resource) {
            is Resource.Success -> if (resource.data != null) Resource.Success(transform(resource.data)) else Resource.Success(null)
            is Resource.Error -> Resource.Error(resource.message, resource.exception)
            is Resource.Loading -> Resource.Loading
        }
    }

/**
 * Utility extension for handling Resource flows
 * Catches any exceptions and converts them to Resource.Error
 */
fun <T> Flow<Resource<T>>.catchToResourceError(): Flow<Resource<T>> = 
    this.catch { e ->
        emit(Resource.Error(e.message ?: "Unknown error", e as? Exception))
    }

/**
 * Utility extension for handling Resource flows
 * Applies the given side effect to the data in Success state
 */
fun <T> Flow<Resource<T>>.onSuccessData(action: suspend (T) -> Unit): Flow<Resource<T>> = 
    this.onEach { resource ->
        if (resource is Resource.Success) {
            action(resource.data)
        }
    } 