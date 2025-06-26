package org.simpmusic.lyrics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
class LyricsApplication

fun main(args: Array<String>) {
    runApplication<LyricsApplication>(*args)
}
