package hu.bme.aut.mantella.screens.markdownViewerScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.bme.aut.mantella.data.NextcloudRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MarkdownViewerViewModel(
    private val repo: NextcloudRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val markdownText: String = ""
    )

    fun loadFile(pagePath: String) {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            runCatching {
                repo.getMarkdownFile(pagePath)
            }
                .onSuccess { text ->
                    if (text.isEmpty()) {
                        println("Empty file")
                    }
                    else {
                        println("File content")
                    }
                    _uiState.value = UiState(isLoading = false, markdownText = text)
                }
                .onFailure { e ->
                    println("Error loading file: ${e.message}")
                    _uiState.value = UiState(error = e.message)
                }
        }
    }

    fun onTextEdit(newText: String) {
        _uiState.value = _uiState.value.copy(markdownText = newText)
    }

    fun commitChanges(pagePath: String) {
        val currentText = uiState.value.markdownText
        viewModelScope.launch {
            runCatching {
                repo.saveMarkdownFile(pagePath, currentText)
            }.onFailure {
                println("Failed to commit file: ${it.message}")
            }
        }
    }
}