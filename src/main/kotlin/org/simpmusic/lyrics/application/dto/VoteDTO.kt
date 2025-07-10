package org.simpmusic.lyrics.application.dto

/**
 * Data Transfer Object for vote actions
 * Used to receive vote requests from clients
 */
data class VoteDTO(
    val id: String,
    val vote: Int // 1 for upvote, 0 for downvote
) 