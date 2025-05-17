package hu.bme.aut.mantella.model

import kotlinx.serialization.Serializable

@Serializable
data class OcsRoot<T>(
    val ocs: OcsEnvelope<T>
)

@Serializable
data class OcsEnvelope<T>(
    val data: T
)

@Serializable
data class UsersPayload(val users: List<String>)

@Serializable
data class GroupsPayload(val groups: List<String>)
