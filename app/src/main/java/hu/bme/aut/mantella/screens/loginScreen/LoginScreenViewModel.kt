package hu.bme.aut.mantella.screens.loginScreen


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.bme.aut.mantella.data.CredentialStore
import hu.bme.aut.mantella.data.NextcloudApi
import hu.bme.aut.mantella.model.AuthCredentials
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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