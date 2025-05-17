package hu.bme.aut.mantella.screens.newCollectiveScreen

import android.util.Log.e
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.bme.aut.mantella.data.CollectivePagesCache.getCollectiveNames
import hu.bme.aut.mantella.data.CredentialStore
import hu.bme.aut.mantella.data.NextcloudRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewCollectiveViewModel(
    private val repo: NextcloudRepo,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val usersAndGroups: Pair<List<String>, List<String>> = Pair(emptyList(), emptyList()),
        val nameAvailable: Boolean = true,
        val userName: String = ""
    )

    init {
        loadUsersAndGroups()
        getUsername()
    }

    fun checkAvailability(name: String) {
        if (getCollectiveNames().contains(name.lowercase().trim())) {
            _uiState.value = _uiState.value.copy(nameAvailable = false)
        }
        else {
            _uiState.value = _uiState.value.copy(nameAvailable = true)
        }
    }

    fun loadUsersAndGroups() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }

        try {
            val (users, groups) = repo.getAllUsersAndGroups()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    usersAndGroups = users to groups
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }

    fun saveCollective(
        emoji: String,
        name: String,
        users: List<String>,
        groups: List<String>
    ) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }

        try {
            repo.createCollectiveOfficial(
                title = name.trim(),
                emoji = emoji,
                users = users,
                groups = groups
            )
            _uiState.update { it.copy(isLoading = false) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Save failed"
                )
            }
        }
    }

    fun getUsername() = viewModelScope.launch {
        _uiState.update {
            it.copy(
                userName = credentialStore.get()?.username ?: ""
            )
        }
    }

}