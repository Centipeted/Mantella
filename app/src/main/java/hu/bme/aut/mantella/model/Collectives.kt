package hu.bme.aut.mantella.model

import kotlinx.serialization.Serializable

@Serializable
data class CollectivesEmojiResponse(
    val data: List<CollectiveWithEmoji>
)

@Serializable
data class CollectiveWithEmoji(
    val name: String,
    val emoji: String,
)