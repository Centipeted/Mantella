package hu.bme.aut.mantella.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import hu.bme.aut.mantella.model.AuthCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CredentialStore {
    suspend fun save(creds: AuthCredentials)
    suspend fun get(): AuthCredentials?
    suspend fun clear()
}

class SecureCredentialStore(context: Context) : CredentialStore {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private companion object {
        const val KEY_SERVER   = "server"
        const val KEY_USERNAME = "username"
        const val KEY_TOKEN    = "token"
    }


    override suspend fun save(creds: AuthCredentials) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_SERVER,   creds.server)
            .putString(KEY_USERNAME, creds.username)
            .putString(KEY_TOKEN,    creds.appPassword)
            .apply()
    }

    override suspend fun get(): AuthCredentials? = withContext(Dispatchers.IO) {
        val server = prefs.getString(KEY_SERVER,   null)
        val user   = prefs.getString(KEY_USERNAME, null)
        val token  = prefs.getString(KEY_TOKEN,    null)

        if (server != null && user != null && token != null)
            AuthCredentials(server, user, token)
        else
            null
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
}