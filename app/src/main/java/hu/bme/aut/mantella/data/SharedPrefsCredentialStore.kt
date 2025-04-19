package hu.bme.aut.mantella.data

import android.content.Context
import hu.bme.aut.mantella.screens.loginScreen.AuthCredentials
import hu.bme.aut.mantella.screens.loginScreen.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SharedPrefsCredentialStore(
    context: Context
) : CredentialStore {

    private val prefs = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    override suspend fun save(creds: AuthCredentials) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString("server",   creds.server)
            .putString("username", creds.username)
            .putString("token",    creds.appPassword)
            .apply()
    }

    override suspend fun get(): AuthCredentials? = withContext(Dispatchers.IO) {
        val server = prefs.getString("server",   null)
        val user   = prefs.getString("username", null)
        val token  = prefs.getString("token",    null)

        if (server != null && user != null && token != null)
            AuthCredentials(server, user, token)
        else
            null
    }
}