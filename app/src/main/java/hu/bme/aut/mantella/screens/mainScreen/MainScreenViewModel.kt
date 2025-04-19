import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.bme.aut.mantella.data.NextcloudRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainScreenViewModel(
    private val repo: NextcloudRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val entries: List<String> = emptyList()
    )

    init { loadRootFolder() }

    fun loadRootFolder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching { repo.listRoot() }
                .onSuccess { list ->
                    _uiState.value = UiState(entries = list)      // done
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
}

