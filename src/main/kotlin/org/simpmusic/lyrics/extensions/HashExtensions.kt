package org.simpmusic.lyrics.extensions

fun String.sha256(): String {
    return this.toByteArray().let { byteArray ->
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(byteArray)
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}