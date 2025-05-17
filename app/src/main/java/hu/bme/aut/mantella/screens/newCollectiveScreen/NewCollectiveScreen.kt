package hu.bme.aut.mantella.screens.newCollectiveScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import hu.bme.aut.mantella.ui.theme.AdminBackground
import hu.bme.aut.mantella.util.getRandomEmoji
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCollectiveScreen(
    navController: NavController,
    viewModel: NewCollectiveViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var collectiveName by remember { mutableStateOf("") }
    var collectiveEmoji by remember { mutableStateOf(getRandomEmoji()) }
    val selectedUsers  = remember { mutableStateListOf<String>() }
    val selectedGroups = remember { mutableStateListOf<String>() }

    LaunchedEffect(state.userName) {
        if (state.userName.isNotEmpty() && !selectedUsers.contains(state.userName)) {
            selectedUsers.add(state.userName)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New collective") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = collectiveName.isNotEmpty(),
                        enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                        exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                    ) {
                        IconButton(
                            onClick = { viewModel.saveCollective(
                                    collectiveEmoji,
                                    collectiveName,
                                    selectedUsers,
                                    selectedGroups
                                )
                                navController.navigateUp()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        focusManager.clearFocus()
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(
                    text = collectiveEmoji,
                    fontSize = 36.sp,
                    modifier = Modifier
                        .padding(top = 16.dp)
                )
                OutlinedTextField(
                    label = { Text("name") },
                    value = collectiveName,
                    onValueChange = { value ->
                        collectiveName = value
                        viewModel.checkAvailability(value)
                    },
                    isError = !state.nameAvailable,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .focusRequester(focusRequester)
                )
            }
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    item {
                        Text(
                            text = "Users",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 16.dp)
                        )
                    }

                    items(state.usersAndGroups.first) { user ->
                        val isSelected = user in selectedUsers
                        SelectableListItem(
                            text = user,
                            selected = isSelected,
                            onSelect = {
                                if (isSelected) selectedUsers.remove(user)
                                else selectedUsers.add(user)
                            },
                            user = true
                        )
                    }

                    item {
                        Text(
                            text = "Groups",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 16.dp)
                        )
                    }

                    items(state.usersAndGroups.second) { group ->
                        val isSelected = group in selectedGroups
                        SelectableListItem(
                            text = group,
                            selected = isSelected,
                            onSelect = {
                                if (isSelected) selectedGroups.remove(group)
                                else selectedGroups.add(group)
                            },
                            user = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectableListItem(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit,
    user: Boolean
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
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                onSelect()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (user) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = AdminBackground, shape = CircleShape)
                ) {
                    Text(
                        text = text.first().uppercase(),
                        color = lerp(AdminBackground, Color.Black, 0.3f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp
                    )
                }
            }
            else {
                Icon(Icons.Default.Group, contentDescription = "Back")
            }
            Text(
                text = text,
                modifier = Modifier
                    .padding(start = 8.dp),
                fontSize = 24.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

        }
    }
}