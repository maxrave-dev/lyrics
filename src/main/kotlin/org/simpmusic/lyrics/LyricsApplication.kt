package org.simpmusic.lyrics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@EnableCaching
class LyricsApplication

fun main(args: Array<String>) {
    runApplication<LyricsApplication>(*args)
}
