package hu.bme.aut.mantella.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CollectivesEmojiResponse(
    val data: List<CollectiveWithEmoji>
)

@Serializable
data class CollectiveWithEmoji(
    val name: String,
    val emoji: String,
)

@Serializable
data class CollectivePage(
    val mainPage: Boolean,
    val name: String,
    val path: String
)