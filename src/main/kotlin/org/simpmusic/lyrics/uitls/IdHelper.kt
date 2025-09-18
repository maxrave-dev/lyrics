@file:Suppress("ktlint:standard:filename")

package org.simpmusic.lyrics.uitls

import java.time.Instant
import kotlin.random.Random

class ID {
    companion object {
        // Generate an hex ID based on timestamp
        // Recreated from https://www.php.net/manual/en/function.uniqid.php
        private fun hexTimestamp(): String {
            val now = Instant.now()
            val sec = now.epochSecond
            val usec = (System.nanoTime() / 1000) % 1000

            val hexTimestamp = "%08x%05x".format(sec, usec)

            return hexTimestamp
        }

        fun custom(id: String): String = id

        /**
         * Generate a unique ID with padding to have a longer ID
         *
         * @param padding The number of characters to add to the ID
         * @returns The unique ID
         */
        fun unique(padding: Int = 7): String {
            val baseId = hexTimestamp()
            val randomPadding =
                (1..padding).joinToString("") { Random.nextInt(0, 16).toString(16) }

            return baseId + randomPadding
        }
    }
}
