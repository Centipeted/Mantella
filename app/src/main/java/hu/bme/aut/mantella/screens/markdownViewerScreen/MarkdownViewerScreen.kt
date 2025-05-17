package hu.bme.aut.mantella.screens.markdownViewerScreen

import android.R.attr.scheme
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownViewerScreen(
    pageName: String,
    pagePath: String,
    navController: NavController,
    viewModel: MarkdownViewerViewModel = koinViewModel()
) {
    LaunchedEffect(pagePath) { viewModel.loadFile(pagePath) }

    BackHandler {
        viewModel.commitChanges(pagePath)
        navController.navigateUp()
    }

    val state by viewModel.uiState.collectAsState()
    var editMode by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val anchors by remember(state.markdownText) {
        mutableStateOf(headingAnchors(state.markdownText))
    }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val fontSizeSp = MaterialTheme.typography.bodyLarge.fontSize
    val lineHeightPx = with(density) {
        (fontSizeSp.value * 1.7f).sp.toPx()
    }.toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageName) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.commitChanges(pagePath)
                            navController.navigateUp()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit page")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 12.dp)
                    ) {
                        IconButton(onClick = { editMode = !editMode }) {
                            Icon(
                                imageVector = if (editMode) Icons.Default.Article else Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            editMode -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    item {
                        BasicTextField(
                            value = state.markdownText,
                            onValueChange = viewModel::onTextEdit,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }

            else -> {
                val codeBg  = MaterialTheme.colorScheme.surfaceVariant
                val codeFg = MaterialTheme.colorScheme.onSurfaceVariant
                val headingLine = MaterialTheme.colorScheme.outline
                val context = LocalContext.current

                LazyColumn(
                    state    = listState,
                    modifier = Modifier.padding(padding)
                ) {
                    item {
                        MarkdownText(
                            markdown = state.markdownText,
                            modifier = Modifier.padding(16.dp),
                            syntaxHighlightColor     = codeBg,
                            syntaxHighlightTextColor = codeFg,
                            headingBreakColor        = headingLine,
                            linkColor                = MaterialTheme.colorScheme.primary,
                            style                    = MaterialTheme.typography.bodyLarge,
                            disableLinkMovementMethod = true,
                            onLinkClicked = { url ->
                                if (url.startsWith("#")) {
                                    anchors[url.drop(1)]?.let { info ->
                                        val y = info.line * lineHeightPx
                                        scope.launch {
                                            listState.animateScrollToItem(0, y)
                                        }
                                    }
                                } else {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private data class AnchorInfo(val line: Int)

private fun headingAnchors(md: String): Map<String, AnchorInfo> {
    val clean   = "[^a-z0-9\\- ]".toRegex(RegexOption.IGNORE_CASE)
    return md.lineSequence()
        .mapIndexedNotNull { idx, line ->
            if (line.startsWith("#")) {
                val slug = line.trimStart('#', ' ')
                    .trim()
                    .lowercase()
                    .replace(clean, "")
                    .replace(' ', '-')
                slug to AnchorInfo(idx)
            } else null
        }
        .toMap()
}