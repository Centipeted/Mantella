package hu.bme.aut.mantella.model

data class AuthCredentials(
    val server: String,
    val username: String,
    val appPassword: String
)