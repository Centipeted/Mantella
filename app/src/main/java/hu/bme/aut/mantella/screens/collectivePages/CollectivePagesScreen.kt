package hu.bme.aut.mantella.screens.collectivePages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import hu.bme.aut.mantella.model.CollectivePage
import hu.bme.aut.mantella.navigation.Screen
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectivePagesScreen(
    collectiveName: String,
    collectiveEmoji: String,
    navController: NavController,
    viewModel: CollectivePagesViewModel = koinViewModel()
) {
    LaunchedEffect(collectiveName) {
        viewModel.loadCollectivePages(collectiveName)
    }

    val state by viewModel.uiState.collectAsState()
    var newItemVisible by remember { mutableStateOf(false) }
    val selectedPages = remember { mutableStateListOf<CollectivePage>() }
    val selectMode = selectedPages.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$collectiveEmoji $collectiveName") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = selectMode,
                        enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                        exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                    ) {
                        IconButton(onClick = { viewModel.deleteSelectedPages(selectedPages) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected pages",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .size(28.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.entries.isEmpty()) {
            Text(
                "No pages",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
        }
        else {
            Column(modifier = Modifier.padding(padding)) {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, bottom = 16.dp, top = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { newItemVisible = !newItemVisible }
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (newItemVisible) Icons.Default.Remove else Icons.Default.Add,
                        contentDescription = "Add new page",
                        tint = Color.White,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
                    )
                    Text(
                        text = "New page",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
                    )
                }
                LazyColumn {
                    items(
                        items = state.entries,
                        key   = { it.path }
                    ) { page ->

                        val isSelected = page in selectedPages

                        PageListItem(
                            collectivePage = page,
                            collectiveName = collectiveName,
                            collectiveEmoji = collectiveEmoji,
                            selectMode = selectMode,
                            selected = isSelected,
                            onClick = {
                                if (selectMode) {
                                    if (isSelected) {
                                        selectedPages.remove(page)
                                    } else {
                                        if (!page.mainPage) selectedPages.add(page)
                                    }
                                } else {
                                    navController.navigate(
                                        if (page.mainPage) {
                                            Screen.MarkdownViewerScreen
                                                .createRoute(CollectivePage(true, collectiveName, page.path))
                                        } else {
                                            Screen.MarkdownViewerScreen.createRoute(page)
                                        }
                                    )
                                }
                            },
                            onLongClick = {
                                if (!selectMode) {
                                    selectedPages.clear()
                                    selectedPages.add(page)
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                    item {
                        if (newItemVisible) {
                            val existingNames = state.entries.map { it.name.lowercase() }
                            var newItemName by remember { mutableStateOf("") }
                            var isError by remember { mutableStateOf(false) }

                            val focus = remember { FocusRequester() }
                            LaunchedEffect(Unit) { focus.requestFocus() }

                            val addPage = {
                                val candidate = newItemName.trim()

                                when {
                                    candidate.isBlank() -> {
                                        newItemVisible = false
                                        isError = false
                                    }
                                    candidate in existingNames -> {
                                        isError = true
                                    }
                                    "Readme" in existingNames -> {
                                        isError = true
                                    }
                                    else -> {
                                        viewModel.addPage(candidate, collectiveName)
                                        newItemName = ""
                                        newItemVisible = false
                                        isError = false
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 2.dp,
                                        color = if (isError) MaterialTheme.colorScheme.error else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .background(
                                        lerp(
                                            MaterialTheme.colorScheme.background,
                                            Color.Black,
                                            0.2f
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    BasicTextField(
                                        value = newItemName,
                                        onValueChange = {
                                            newItemName = it
                                            if (isError) isError = false
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions.Default.copy(
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { addPage() }
                                        ),
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .weight(1f)
                                            .focusRequester(focus)
                                    )

                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Confirm",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { addPage() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageListItem(
    collectivePage: CollectivePage,
    collectiveName: String,
    collectiveEmoji: String,
    selectMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetColor = if (!selected) {
        lerp(MaterialTheme.colorScheme.background, Color.Black, 0.2f)
    } else {
        lerp(MaterialTheme.colorScheme.background, Color.White, 0.1f)
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(
            durationMillis = 100,
            easing = FastOutLinearInEasing
        ),
        label = "backgroundColor"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = {
                    onClick()
                },
                onLongClick = {
                    if (!collectivePage.mainPage) onLongClick()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (collectivePage.mainPage) {
                Text(
                    text = collectiveEmoji,
                    fontSize = 24.sp,
                )
            }
            else {
                if (selectMode) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.CheckBox,
                            contentDescription = "Checked page",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(20.dp)
                        )
                    }
                    else {
                        Icon(
                            imageVector = Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Unchecked page",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(20.dp)
                        )
                    }
                }
                else {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Markdown document",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(20.dp)
                    )
                }
            }
            Text(
                text = if (collectivePage.mainPage) collectiveName else collectivePage.name,
                modifier = Modifier
                    .padding(start = 8.dp),
                fontSize = if (collectivePage.mainPage) 24.sp else 20.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

        }
    }
}
