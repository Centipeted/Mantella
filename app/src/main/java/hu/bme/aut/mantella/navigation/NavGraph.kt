package hu.bme.aut.mantella.navigation

import android.content.Context
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import hu.bme.aut.mantella.screens.collectivePages.CollectivePagesScreen
import hu.bme.aut.mantella.screens.loginScreen.LoginScreen
import hu.bme.aut.mantella.screens.mainScreen.MainScreen
import hu.bme.aut.mantella.screens.markdownViewerScreen.MarkdownViewerScreen
import hu.bme.aut.mantella.screens.newCollectiveScreen.NewCollectiveScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private fun computeStartDestination(context: Context): String {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val hasCredentials =
        prefs.getString("server",   null).isNullOrBlank().not() &&
        prefs.getString("username", null).isNullOrBlank().not() &&
        prefs.getString("token",    null).isNullOrBlank().not()

    return if (hasCredentials) Screen.MainScreen.route else Screen.LoginScreen.route
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val startDestination = remember { computeStartDestination(context) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition  = { fadeOut(tween(300)) }
    ) {
        composable(Screen.MainScreen.route)  { MainScreen(navController) }
        composable(Screen.LoginScreen.route) { LoginScreen(navController) }
        composable(
            route = Screen.CollectivePagesScreen.route,
            arguments = listOf(
                navArgument("collectiveName") { type = NavType.StringType },
                navArgument("collectiveEmoji") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val collectiveName = backStackEntry.arguments?.getString("collectiveName") ?: return@composable
            val collectiveEmoji = backStackEntry.arguments?.getString("collectiveEmoji") ?: return@composable
            CollectivePagesScreen(collectiveName, collectiveEmoji, navController = navController)
        }
        composable(
            route = Screen.MarkdownViewerScreen.route,
            arguments = listOf(
                navArgument("pageName") { type = NavType.StringType },
                navArgument("pagePath") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val pageName = backStackEntry.arguments?.getString("pageName") ?: return@composable
            val encodedPagePath = backStackEntry.arguments?.getString("pagePath") ?: return@composable
            val decodedPagePath = URLDecoder.decode(encodedPagePath, StandardCharsets.UTF_8.toString())

            MarkdownViewerScreen(pageName, decodedPagePath, navController = navController)
        }
        composable(Screen.NewCollectiveScreen.route) { NewCollectiveScreen(navController) }
    }
}