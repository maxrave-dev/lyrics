package org.simpmusic.lyrics.application.dto.request

/**
 * @param vote 1 for upvote, 0 for downvote
 */
data class VoteRequestDTO(
    val id: String,
    val vote: Int,
) : BaseRequestDTO {
    override fun getUniqueId(): String = "$id-$vote"
}
