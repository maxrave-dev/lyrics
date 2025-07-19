# API Response Standardization - July 15, 2023

## Overview

We have standardized the API response structure by implementing a polymorphic type `ApiResult<T>` for all API endpoints. This enhances consistency and makes handling API responses more robust for API consumers.

## Changes

### New Components

1. **ApiResult<T>** - A sealed class with three subclasses:
   - `ApiResult.Success<T>`: Contains successful response data
   - `ApiResult.Error`: Contains detailed error information
   - `ApiResult.Loading<T>`: Contains information about processing state

2. **LoadingResponseDTO** - A new DTO for processing state information

3. **Extension Functions** - For seamless conversion between Resource and ApiResult

### Implementation Details

#### ApiResult<T>

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(
        val data: T,
        val success: Boolean = true,
    ) : ApiResult<T>()

    data class Error(
        val error: ErrorResponseDTO,
        val success: Boolean = false,
    ) : ApiResult<Nothing>()

    data class Loading<T>(
        val processing: LoadingResponseDTO,
        val success: Boolean = false,
    ) : ApiResult<T>()
    
    // Factory methods in companion object
}
```

#### LoadingResponseDTO

```kotlin
data class LoadingResponseDTO(
    val code: Int,
    val message: String,
) : BaseResponseDTO {
    companion object {
        fun fromMessage(code: Int = HttpStatus.PROCESSING.value(), message: String): LoadingResponseDTO {
            return LoadingResponseDTO(code = code, message = message)
        }
    }
}
```

### Controller Modifications

All endpoints in `LyricController` have been updated to return `ResponseEntity<ApiResult<T>>` instead of directly returning data or ErrorResponse:

```kotlin
@GetMapping("/{videoId}")
suspend fun getLyricsByVideoId(
    @PathVariable videoId: String,
    @RequestParam(required = false) limit: Int?,
    @RequestParam(required = false) offset: Int?,
): ResponseEntity<ApiResult<List<LyricResponseDTO>>> =
    withContext(ioDispatcher) {
        logger.debug("getLyricsByVideoId --> Getting lyrics for videoId: $videoId, limit: $limit, offset: $offset")
        val result = lyricService.getLyricsByVideoId(videoId, limit, offset).last()
        when (result) {
            is Resource.Success -> {
                // Handle success with empty check
                if (result.data.isNotEmpty()) {
                    ResponseEntity.ok(ApiResult.Success(data = result.data))
                } else {
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResult.Error(ErrorResponseDTO.notFound("Lyrics not found for videoId: $videoId")))
                }
            }
            is Resource.Error -> {
                // Handle error
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.Error(ErrorResponseDTO.serverError("Failed to get lyrics by videoId: $videoId")))
            }
            is Resource.Loading -> {
                // Handle loading state
                ResponseEntity.status(HttpStatus.PROCESSING)
                    .body(ApiResult.Loading(LoadingResponseDTO.fromMessage(
                        HttpStatus.PROCESSING.value(), 
                        "Processing request to get lyrics for videoId: $videoId"
                    )))
            }
        }
    }
```

## API Response Format

### Success Response

```json
{
  "data": [...],
  "success": true
}
```

### Error Response

```json
{
  "error": {
    "error": true,
    "code": 404,
    "reason": "Lyrics not found for videoId: abc123"
  },
  "success": false
}
```

### Loading Response

```json
{
  "processing": {
    "code": 102,
    "message": "Processing request to get lyrics for videoId: abc123"
  },
  "success": false
}
```

## Modified Endpoints

All API endpoints maintain their original URLs and methods but now return a consistent response structure:

1. `GET /v1/{videoId}` - Get lyrics by videoId
2. `GET /v1/search/title` - Find lyrics by song title
3. `GET /v1/search/artist` - Find lyrics by artist
4. `GET /v1/search` - Search lyrics by keywords
5. `POST /v1` - Create new lyrics
6. `GET /v1/translated/{videoId}` - Get translated lyrics by videoId
7. `GET /v1/translated/{videoId}/{language}` - Get translated lyrics by videoId and language
8. `POST /v1/translated` - Create new translated lyrics
9. `POST /v1/vote` - Vote for lyrics
10. `POST /v1/translated/vote` - Vote for translated lyrics

## Benefits

1. **Consistency** - All API responses follow a uniform structure
2. **Type Safety** - Clear types make response handling safer for clients
3. **Extensibility** - Easy to extend the API system with new states if needed
4. **Clarity** - Clear, detailed, and consistent error messages
5. **Processing State** - Support for processing state for long-running requests

## Integration Guide

### Backend Integration

When creating new controllers, follow this pattern:

```kotlin
val apiResult = result.toApiResult(
    successHandler = { data ->
        if (isDataValid(data)) {
            ApiResult.success(data)
        } else {
            ApiResult.notFound("Data not found for param: $param")
        }
    },
    errorHandler = {
        logger.error("endpoint --> Failed: ${this.message}", this.exception)
        ApiResult.serverError("Failed to process request")
    }
)

// Return ResponseEntity with appropriate status code
when (apiResult) {
    is ApiResult.Success -> ResponseEntity.ok(apiResult)
    is ApiResult.Error -> ResponseEntity.status(apiResult.error.code).body(apiResult)
    is ApiResult.Loading -> ResponseEntity.status(HttpStatus.PROCESSING).body(apiResult)
}
```

### Client Integration

#### Kotlin Client

```kotlin
when (response) {
    is ApiResult.Success -> {
        // Handle successful data
        val data = response.data
        // ...
    }
    is ApiResult.Error -> {
        // Handle error
        val errorCode = response.error.code
        val errorMessage = response.error.reason
        // ...
    }
    is ApiResult.Loading -> {
        // Handle processing state
        val processingMessage = response.processing.message
        // ...
    }
}
```

#### JavaScript/TypeScript Client

```javascript
if (response.success) {
  // Handle successful data
  const data = response.data;
  // ...
} else if (response.error) {
  // Handle error
  const errorCode = response.error.code;
  const errorMessage = response.error.reason;
  // ...
} else if (response.processing) {
  // Handle processing state
  const processingMessage = response.processing.message;
  // ...
}
```

#### React Client with Polling for Processing State

```typescript
async function fetchWithPolling<T>(url: string, maxAttempts = 10, delayMs = 2000): Promise<T> {
  let attempts = 0;
  
  while (attempts < maxAttempts) {
    const response = await fetch(url);
    const result: ApiResult<T> = await response.json();
    
    if (result.success) {
      return result.data;
    } else if ('error' in result) {
      throw new Error(result.error.reason);
    }
    
    // Processing state, wait and try again
    console.log(`Request processing: ${result.processing.message}. Attempt ${attempts + 1}/${maxAttempts}`);
    attempts++;
    
    if (attempts < maxAttempts) {
      await new Promise(resolve => setTimeout(resolve, delayMs));
    } else {
      throw new Error('Request processing timed out');
    }
  }
  
  throw new Error('Request processing timed out');
}
```

## Conclusion

The new `ApiResult` structure provides a consistent way to handle API responses, including success, error, and processing states. This standardization improves the API's robustness and usability across the entire platform.