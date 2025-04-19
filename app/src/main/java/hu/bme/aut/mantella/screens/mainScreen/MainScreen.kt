package hu.bme.aut.mantella.screens.mainScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import MainScreenViewModel
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.items

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = koinViewModel()
) {
    val ui by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Root folder", style = MaterialTheme.typography.titleLarge)

        when {
            ui.isLoading -> {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
            }
            ui.error != null -> {
                Spacer(Modifier.height(16.dp))
                Text(ui.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> {
                Spacer(Modifier.height(8.dp))
                if (ui.entries.isEmpty()) {
                    Text("— empty —", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(ui.entries) { name ->
                            Text(
                                name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
