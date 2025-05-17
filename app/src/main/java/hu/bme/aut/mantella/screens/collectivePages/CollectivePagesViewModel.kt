package hu.bme.aut.mantella.screens.collectivePages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.bme.aut.mantella.data.CollectivePagesCache
import hu.bme.aut.mantella.data.NextcloudRepo
import hu.bme.aut.mantella.model.CollectivePage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollectivePagesViewModel(
    private val repo: NextcloudRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val entries: List<CollectivePage> = emptyList()
    )

    private fun List<CollectivePage>.ordered(): List<CollectivePage> {
        return this.sortedBy { it.name.lowercase() }
            .sortedByDescending { it.mainPage }
    }


    fun loadCollectivePages(collectiveName: String) = viewModelScope.launch {
        _uiState.value = UiState(isLoading = true)
        runCatching { CollectivePagesCache.getPages(collectiveName) }
            .onSuccess { pages ->
                _uiState.value = UiState(entries = pages.ordered())
            }
            .onFailure { e ->
                _uiState.value = UiState(error = e.message)
            }
    }

    fun addPage(pageName: String, collectiveName: String) = viewModelScope.launch {
        val newPage = CollectivePage(
            mainPage = false,
            name = pageName,
            path = "$collectiveName/$pageName.md"
        )

        runCatching { repo.saveMarkdownFile(newPage.path, "") }
            .onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
                return@launch
            }

        CollectivePagesCache.addPage(collectiveName, newPage)

        val updated = (_uiState.value.entries + newPage).ordered()
        _uiState.value = _uiState.value.copy(entries = updated, error = null)
    }

    fun deleteSelectedPages(selected: List<CollectivePage>, onDone: () -> Unit = {}) = viewModelScope.launch {
        if (selected.isEmpty()) return@launch

        _uiState.update { it.copy(isLoading = true, error = null) }

        val paths = selected.map { it.path }

        runCatching {
            repo.deleteMarkdownFiles(paths)
            CollectivePagesCache.removePages(paths)
        }
            .onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        entries = state.entries.filterNot { it in selected }
                    )
                }
                onDone()
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Delete failed"
                    )
                }
            }
    }
}


