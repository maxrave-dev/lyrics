# SimpMusic Lyrics

A robust and scalable RESTful API service for managing song lyrics, translations, and not-found records. Built with Kotlin, Spring Boot, and Appwrite database, featuring advanced duplicate detection and clean architecture patterns.

[![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/) [![Spring Boot](https://img.shields.io/badge/spring%20boot-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot) [![Appwrite](https://img.shields.io/badge/appwrite-%23FD366E.svg?style=for-the-badge&logo=appwrite&logoColor=white)](https://appwrite.io/)

## Features

### Core Functionality
- **Lyrics Management**: Create, read, search, and manage song lyrics
- **Multi-language Support**: Handle translated lyrics in multiple languages
- **Advanced Search**: Search by song title, artist name, or full-text content
- **Not-Found Tracking**: Track videos without available lyrics
- **Duplicate Detection**: SHA256-based content deduplication
- **Real-time Processing**: Asynchronous operations with Kotlin Coroutines

## Installation

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/lyrics-api.git
cd lyrics-api
```

### 2. Set Up Appwrite
1. Create an [Appwrite](https://appwrite.io/) account
2. Create a new project
3. Note down your:
   - Project ID
   - API Endpoint
   - API Key

### 3. Configure Environment
Create `.env` following the `.env.example`

### 4. Build the Application
```bash
# Clean and build
./gradlew clean build

# Run tests
./gradlew test

# Generate JAR
./gradlew bootJar
```

### 5. Run the Application
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

## API Documentation

### Base URL
```
http://localhost:8080/api/lyrics
```

### SwaggerUI
```
http://localhost:8080/swagger-ui/index.html
```

### Endpoints Overview

#### Lyrics Management
```http
GET    /api/lyrics/{videoId}           # Get lyrics by video ID
GET    /api/lyrics/search/title?title= # Search by song title
GET    /api/lyrics/search/artist?artist= # Search by artist
GET    /api/lyrics/search?q=           # Full-text search
POST   /api/lyrics                     # Create new lyrics
DELETE /api/lyrics/{id}                # Delete lyrics
```

#### Translated Lyrics
```http
GET    /api/lyrics/translated/{videoId}    # Get translations
GET    /api/lyrics/translated/{videoId}/{language} # Get specific translation
POST   /api/lyrics/translated              # Create translation
DELETE /api/lyrics/translated/{id}         # Delete translation
```

#### Admin Operations
```http
POST   /api/admin/initialize              # Initialize database
POST   /api/admin/clear                   # Clear all data
POST   /api/admin/rebuild                 # Rebuild database
```

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
<a href="https://crowdin.com">
<img src="https://support.crowdin.com/assets/logos/plate/png/crowdin-logo-with-plate.png" width="300"/>
</a>
<a href="https://sentry.io">
<img src="https://github.com/maxrave-dev/SimpMusic/blob/dev/asset/sentry.svg?raw=true" width="300"/>
</a>
<br>

Crowdin and Sentry, both have free enterprise plan for Open-source project. Follow the URLs: 
- [Open Source License Request Form | Crowdin](https://crowdin.com/page/open-source-project-setup-request)
- [Sentry for Open Source | Sentry](https://sentry.io/for/open-source/)

*This project is a part of SimpMusic.org Open-source project by me [maxrave-dev](https://github.com/maxrave-dev)*