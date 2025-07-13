package org.simpmusic.lyrics.application.dto.request

data class VoteRequestDTO(
    val id: String,
    val vote: Int // 1 for upvote, 0 for downvote
)