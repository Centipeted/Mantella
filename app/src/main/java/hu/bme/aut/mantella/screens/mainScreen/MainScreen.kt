package hu.bme.aut.mantella.screens.mainScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import hu.bme.aut.mantella.navigation.Screen
import hu.bme.aut.mantella.ui.theme.AdminBackground
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainScreenViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var accountPageVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCollectivesFolder()
        viewModel.cacheCollectivePages()
    }

    Box(
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.height(100.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text(
                                text = "My Collectives",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(end = 12.dp)
                        ) {
                            IconButton(onClick = {  }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .size(28.dp)
                                )
                            }
                            IconButton(
                                onClick = { accountPageVisible = !accountPageVisible },
                                modifier = Modifier
                                    .padding(start = 8.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(color = AdminBackground, shape = CircleShape)
                                ) {
                                    Text(
                                        text = state.usernameFirstLetter.uppercase(),
                                        color = lerp(AdminBackground, Color.Black, 0.3f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.secondary,
                        actionIconContentColor = MaterialTheme.colorScheme.secondary,
                        navigationIconContentColor = MaterialTheme.colorScheme.secondary
                    )
                )
            },
            modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        accountPageVisible = false
                    }
                )
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding)
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { navController.navigate(Screen.NewCollectiveScreen.route) }
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add new collective",
                        tint = Color.White,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
                    )
                    Text(
                        text = "New collective",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
                    )
                }
                val showList = !state.isLoading && state.entries.isNotEmpty()

                when {
                    state.isLoading -> {
                        CollectivesSkeleton()
                    }

                    state.entries.isEmpty() -> {
                        Text(
                            text = "No collectives yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showList,
                    enter = fadeIn(animationSpec = tween(250)),
                    exit  = ExitTransition.None
                ) {
                    LazyColumn {
                        items(state.entries) { collective ->
                            CollectivesListItem(
                                name  = collective.name,
                                emoji = collective.emoji,
                                onClick = {
                                    navController.navigate(
                                        Screen.CollectivePagesScreen.createRoute(collective)
                                    )
                                }
                            )
                        }
                    }
                }

            }
        }

        AccountMenu(
            accountPageVisible = accountPageVisible,
            state = state,
            viewModel = viewModel,
            navController = navController
        )
    }
}

@Composable
fun AccountMenu(
    accountPageVisible: Boolean,
    state: MainScreenViewModel.UiState,
    viewModel: MainScreenViewModel,
    navController: NavController
) {
    AnimatedVisibility(
        visible = accountPageVisible,
        enter = fadeIn() + expandIn(expandFrom = Alignment.TopCenter),
        exit = shrinkOut(shrinkTowards = Alignment.TopCenter) + fadeOut(),
    ) {
        val lerpColor = if(isSystemInDarkTheme()) Color.White else Color.Black
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .padding(24.dp)
                .clip(RoundedCornerShape(10.dp)),
            color = lerp(MaterialTheme.colorScheme.background, lerpColor, 0.2f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .background(AdminBackground, CircleShape)
                    ) {
                        Text(
                            text = state.usernameFirstLetter.uppercase(),
                            color = lerp(AdminBackground, Color.Black, 0.3f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Column {
                        Text(
                            text = state.usernameAndAddress.first,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = state.usernameAndAddress.second,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            viewModel.logout()
                            navController.navigate(Screen.LoginScreen.route)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log out",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Log out",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun CollectivesListItem(
    name: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(
                color = lerp(MaterialTheme.colorScheme.background, Color.Black, 0.2f),
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp,
            )
            Text(
                text = name,
                modifier = Modifier
                    .padding(start = 8.dp),
                fontSize = 24.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

        }
    }
}

@Composable
fun CollectivesSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        repeat(10) { index ->
            val opacity = (-sqrt(index / 6.15f) + 1f).coerceIn(0f, 1f)
            CollectivesSkeletonListItem(opacity)
        }

    }
}

@Composable
fun CollectivesSkeletonListItem(
    opacity: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        LoadingElement(44.8.dp, opacity)
    }
}

@Composable
fun LoadingElement(
    height: Dp,
    opacity: Float
) {
    val infiniteTransition = rememberInfiniteTransition()

    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    Box(
        modifier = Modifier
            .graphicsLayer { alpha = opacity }
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(lerp(MaterialTheme.colorScheme.background, Color.Black, 0.2f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shimmerWidth = size.width / 2

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    start = Offset(animatedOffset * size.width, 0f),
                    end = Offset(animatedOffset * size.width + shimmerWidth, size.height)
                ),
                cornerRadius = CornerRadius(10f, 10f),
                size = Size(size.width, size.height)
            )
        }
    }
}

