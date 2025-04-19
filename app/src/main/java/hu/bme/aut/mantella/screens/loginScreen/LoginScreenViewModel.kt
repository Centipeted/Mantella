package hu.bme.aut.mantella.screens.loginScreen


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AuthCredentials(
    val server: String,
    val username: String,
    val appPassword: String
)

object NextcloudApi {

    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("OCS-APIRequest", "true")
                    .build()
            )
        }
        .build()

    suspend fun generateAppPassword(
        server: String,
        username: String,
        password: String
    ): String = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val url = server.trimEnd('/') + "/ocs/v2.php/core/getapppassword"

        val req: Request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(username, password))
            .header("User-Agent", "NextcloudAndroidSample/1.0")
            .build()

        client.newCall(req).execute().use { resp ->
            when (resp.code) {
                200 -> {
                    val xml = resp.body?.string()
                        ?: throw IOException("Empty body from server")
                    Regex("<apppassword>(.*)</apppassword>")
                        .find(xml)?.groupValues?.get(1)
                        ?: throw IOException("Token not present in response")
                }
                403 -> password // we were already using an app‑password
                else -> throw IOException("HTTP ${resp.code}: ${resp.message}")
            }
        }
    }
}

interface CredentialStore {
    suspend fun save(creds: AuthCredentials)
    suspend fun get(): AuthCredentials?
}

class LoginScreenViewModel(
    private val credentialStore: CredentialStore
) : ViewModel() {

    data class UiState(
        val server: String = "",
        val username: String = "",
        val password: String = "",
        val loading: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    sealed class Event { object LoginSuccess : Event() }
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onServerChange(v: String)   = _state.update { it.copy(server = v) }
    fun onUsernameChange(v: String) = _state.update { it.copy(username = v) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v) }

    fun login() {
        val s = _state.value
        if (s.server.isBlank() || s.username.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "All fields are required.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            runCatching {
                NextcloudApi.generateAppPassword(s.server, s.username, s.password)
            }.onSuccess { token ->
                credentialStore.save(AuthCredentials(s.server, s.username, token))
                _state.update { it.copy(loading = false, password = "") }
                _events.send(Event.LoginSuccess)
            }.onFailure { e ->
                Log.e("LoginVM", "login failed", e)
                _state.update { it.copy(loading = false, error = e.localizedMessage ?: "Unknown error") }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}

class AuthInterceptor(private val credentialStore: CredentialStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = runBlocking {
        val creds = credentialStore.get()
        val reqBuilder = chain.request().newBuilder()
        creds?.let {
            reqBuilder.header(
                "Authorization",
                Credentials.basic(it.username, it.appPassword)
            )
        }
        reqBuilder.header("OCS-APIRequest", "true")
        chain.proceed(reqBuilder.build())
    }
}