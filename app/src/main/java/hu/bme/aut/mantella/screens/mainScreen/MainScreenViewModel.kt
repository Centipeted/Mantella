package hu.bme.aut.mantella.screens.mainScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.bme.aut.mantella.data.CollectivePagesCache
import hu.bme.aut.mantella.data.CollectivePagesCache.clearCache
import hu.bme.aut.mantella.data.CredentialStore
import hu.bme.aut.mantella.data.NextcloudApi
import hu.bme.aut.mantella.data.NextcloudRepo
import hu.bme.aut.mantella.model.CollectiveWithEmoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainScreenViewModel(
    private val repo: NextcloudRepo,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    data class UiState(
        val isLoading: Boolean = true,
        val error: String? = null,
        val entries: List<CollectiveWithEmoji> = emptyList(),
        val usernameFirstLetter: String = "",
        val usernameAndAddress: Pair<String, String> = Pair("", "")
    )

    init {
        viewModelScope.launch(Dispatchers.Default) {
            launch { getUsernameFirstLetter() }
            launch { getUsernameAndAddress() }
            launch { loadCollectivesFolder() }
            launch { cacheCollectivePages() }
        }
    }

    fun loadCollectivesFolder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                val names = repo.listCollectives()
                val emojiMap = repo.getCollectivesEmojis().toMap()
                val pages = repo.fetchCollectivesWithPages()
                println("pages: $pages")
                names.map { name ->
                    CollectiveWithEmoji(
                        name  = name,
                        emoji = emojiMap[name] ?: ""
                    )
                }
            }
                .onSuccess { merged ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error     = null,
                            entries   = merged
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.localizedMessage ?: "Couldn’t load folder."
                        )
                    }
                }
        }
    }

    fun getUsernameAndAddress() {
        viewModelScope.launch {
            val username = credentialStore.get()?.username ?: ""
            val serverName = credentialStore.get()?.server?.split("/")?.last() ?: ""
            _uiState.update {
                it.copy(
                    usernameAndAddress = Pair(username, serverName)
                )
            }
        }
    }

    fun getUsernameFirstLetter() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    usernameFirstLetter = (credentialStore.get()?.username?.first() ?: ' ').toString()
                )
            }
        }
    }

    fun cacheCollectivePages() {
        viewModelScope.launch {
            val pages = repo.fetchCollectivesWithPages()
            CollectivePagesCache.insertCollectives(pages)
        }
    }

    fun logout() {
        viewModelScope.launch {
            credentialStore.get()?.let { credentials ->
                runCatching { NextcloudApi.revokeAppPassword(credentials) }
                    .onFailure { e -> println("Revoke error: ${e.message}") }
            }

            credentialStore.clear()
            clearCache()

            _uiState.update {
                it.copy(
                    usernameAndAddress = "" to "",
                    usernameFirstLetter = "",
                    entries = emptyList(),
                    isLoading = false,
                    error = null
                )
            }
        }
    }
}

