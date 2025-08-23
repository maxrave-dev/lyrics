# SimpMusic Lyrics

A robust and scalable RESTful API service for managing song lyrics, translations, and not-found records. Built with
Kotlin, Spring Boot, Appwrite database and Meilisearch for searching, featuring advanced duplicate detection and clean
architecture patterns.

SimpMusic Lyrics is focusing on YouTube Music, providing data from `videoId` of the track. The database is populated by
the community, SimpMusic app users, and through automated crawling of other web services.

***The app is in alpha phase, the APIs will be changed***

### Main endpoint: HTTPS only

```
https://api-lyrics.simpmusic.org/v1
```

### Web client:

- [https://lyrics.simpmusic.org/](https://lyrics.simpmusic.org/)

[![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/) [![Spring Boot](https://img.shields.io/badge/spring%20boot-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot) [![Appwrite](https://img.shields.io/badge/appwrite-%23FD366E.svg?style=for-the-badge&logo=appwrite&logoColor=white)](https://appwrite.io/)

## Features

### Core Functionality

- **Lyrics Management**: Create, read, search, and manage song lyrics
- **Super Fast Search**: Powered by Meilisearch for quick full-text search
- **Multi-language Support**: Handle translated lyrics in multiple languages
- **Advanced Search**: Search by song title, artist name, or full-text content
- **Not-Found Tracking**: Track videos without available lyrics
- **Duplicate Detection**: SHA256-based content deduplication
- **Real-time Processing**: Asynchronous operations with Kotlin Coroutines
- **Standardized API Responses**: Consistent response format with type-safe handling of success, error, and processing
  states

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/lyrics-api.git
cd lyrics-api
```

### 2. Set up Sentry (Optional)

- If you want to use Sentry for error tracking, create a Sentry account and get your DSN.

### 3. Host your own Meilisearch or use Cloud

- Note your Meilisearch URL and Master Key
- Using Meilisearch for super fast search capabilities

### 4. Set Up Appwrite

1. Create an [Appwrite](https://appwrite.io/) account
2. Create a new project
3. Note down your:
    - Project ID
    - API Endpoint
    - API Key
    - ADMIN_IPS: List of IPs allowed to perform admin actions without Rate Limiting
    - HMAC_SECRET: A secret key for HMAC authentication (should be kept secret)

### 5. Configure Environment

Create `.env` following the `.env.example`

### 6. Build the Application

```bash
# Clean and build
./gradlew clean build

# Run tests
./gradlew test

# Generate JAR
./gradlew bootJar
```

### 7. Run the Application

```bash
# Development mode
./gradlew bootRun

# Production mode
java -jar build/libs/lyrics-<version>.jar
```

### Database Setup

The application automatically creates required collections:

- `lyrics` - Main lyrics collection
- `translated_lyrics` - Translated lyrics collection
- `notfound_lyrics` - Not found tracking collection
- `notfound_translated_lyrics` - Not found translation collection

## API Documentation

### Base URL

```
http://localhost:8080/v1
```

### SwaggerUI

```
http://localhost:8080/swagger-ui/index.html
```

### Endpoints Overview

#### Lyrics Management

```http
# All endpoints now return ApiResult<T> wrapper with standardized response format

GET    /v1/{videoId}           # Get lyrics by video ID
GET    /v1/{videoId}?limit=10&offset=0 # Get paginated lyrics by video ID
GET    /v1/search/title?title= # Search by song title
GET    /v1/search/title?title=&limit=10&offset=0 # Paginated search by song title
GET    /v1/search/artist?artist= # Search by artist
GET    /v1/search/artist?artist=&limit=10&offset=0 # Paginated search by artist
GET    /v1/search?q=           # Full-text search
GET    /v1/search?q=&limit=10&offset=0 # Paginated full-text search
POST   /v1                     # Create new lyrics
```

#### Translated Lyrics

```http
GET    /v1/translated/{videoId}    # Get translations
GET    /v1/translated/{videoId}?limit=10&offset=0 # Get paginated translations
GET    /v1/translated/{videoId}/{language} # Get specific translation
POST   /v1/translated              # Create translation
```

#### Voting

```http
POST   /v1/vote                    # Vote for lyrics -> ApiResult<LyricResponseDTO>
POST   /v1/translated/vote         # Vote for translated lyrics -> ApiResult<TranslatedLyricResponseDTO>
```

### API Response Format

All API endpoints now return responses with a standardized structure using `ApiResult<T>` wrapper:

#### Success Response

```json
{
  "data": [
    ...
  ],
  "success": true
}
```

#### Error Response

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

#### Processing Response

```json
{
  "processing": {
    "code": 102,
    "message": "Processing request to get lyrics for videoId: abc123"
  },
  "success": false
}
```

This standardized format makes it easier to handle API responses consistently across clients. The `success` field
provides a quick way to determine the response type.

## Security Features

### Rate Limiting

The API is protected with rate limiting to prevent abuse:

- 30 requests per minute per IP address
- Applies to all API endpoints
- When limit is exceeded, returns HTTP 429 (Too Many Requests)

### HMAC Authentication

All non-GET requests (POST, PUT, DELETE, PATCH) require HMAC authentication:

1. Generate a timestamp (current time in milliseconds)
2. Create data string: `timestamp + request_uri`
3. Generate HMAC using the shared secret key, checkout `generateHmac` function in `HmacService.kt` for full algorithm.
4. Add headers to your request:
    - `X-Timestamp`: Your timestamp
    - `X-HMAC`: Your generated HMAC token
5. Your HMAC token is available in 5 minutes

Example using curl:

```bash
# Headers required
X-Timestamp: 1630000000000
X-HMAC: base64EncodedHmacToken

# Example command
curl -X POST "http://localhost:8080/v1" \
  -H "Content-Type: application/json" \
  -H "X-Timestamp: 1630000000000" \
  -H "X-HMAC: base64EncodedHmacToken" \
  -d '{"videoId":"dQw4w9WgXcQ", ...}'
```

## Roadmap

- [x] Basic CRUD operations for lyrics
- [x] Security for non-GET requests
- [x] Rate limiting
- [x] Paginated search
- [x] Standardized API response format
- [x] Input data
- [x] Public server
- [x] Frontend integration
- [ ] Automated find not-found lyrics
- [ ] Automated remove negative votes lyrics

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support & Donations

#### Special thanks to all supporter ❤️

 <div align="left"> 
 <a href="https://simpmusic.org/"><img alt="Visit the website" height="50" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/website_vector.svg"></a> &nbsp;        
<a href="https://discord.gg/Rq5tWVM9Hg"><img alt="Discord Server" height="50" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg"></a> &nbsp;        
<br> <a href="https://www.buymeacoffee.com/maxrave"><img alt="Buy me a Coffee" height="50" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/buymeacoffee-singular_vector.svg"></a> &nbsp;        
<a href="https://liberapay.com/maxrave/"><img alt="liberapay" height="50"        
src="https://raw.githubusercontent.com/liberapay/liberapay.com/master/www/assets/liberapay/logo-v2_black-on-yellow.svg"></a> 
</div>

### MOMO or Vietnamese banking

 <p float="left">        
 <img src="https://github.com/maxrave-dev/SimpMusic/blob/dev/asset/52770992.jpg?raw=true" width="300"> 
 </p>

## SimpMusic is sponsored by:

<br />
<a href="https://vercel.com/oss">
  <img alt="Vercel OSS Program" src="https://vercel.com/oss/program-badge.svg" />
</a>
<br />
<br />
<a href="https://www.digitalocean.com/?refcode=d7f6eedfb9a9&utm_campaign=Referral_Invite&utm_medium=Referral_Program&utm_source=badge"><img src="https://web-platforms.sfo2.cdn.digitaloceanspaces.com/WWW/Badge%201.svg" width="300" alt="DigitalOcean Referral Badge" /></a>
<br>
<a href="https://crowdin.com">
<img src="https://support.crowdin.com/assets/logos/plate/png/crowdin-logo-with-plate.png" width="300"/>
</a>
<br>
<a href="https://sentry.io">
<img src="https://github.com/maxrave-dev/SimpMusic/blob/dev/asset/sentry.svg?raw=true" width="300"/>
</a>
<br>

Check out the Vercel open-source program:

- https://vercel.com/open-source-program

Get free $200 credit over 60 days on
DigitalOcean: [GET NOW](https://www.digitalocean.com/?refcode=d7f6eedfb9a9&utm_campaign=Referral_Invite&utm_medium=Referral_Program&utm_source=badge)

Crowdin and Sentry both have a free enterprise plan for Open-source projects. Follow the URLs:

- [Open Source License Request Form | Crowdin](https://crowdin.com/page/open-source-project-setup-request)
- [Sentry for Open Source | Sentry](https://sentry.io/for/open-source/)

*This project is a part of SimpMusic.org Open-source project by me [maxrave-dev](https://github.com/maxrave-dev)*
