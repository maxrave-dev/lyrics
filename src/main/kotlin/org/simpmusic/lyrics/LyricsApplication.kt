package org.simpmusic.lyrics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LyricsApplication

fun main(args: Array<String>) {
    runApplication<LyricsApplication>(*args)
}
