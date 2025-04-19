package hu.bme.aut.mantella.navigation

import android.content.Context
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import hu.bme.aut.mantella.screens.loginScreen.LoginScreen
import hu.bme.aut.mantella.screens.mainScreen.MainScreen

private fun computeStartDestination(ctx: Context): String {
    val prefs = ctx.getSharedPreferences("credentials", Context.MODE_PRIVATE)
    val hasCreds = prefs.contains("server") &&
            prefs.contains("username") &&
            prefs.contains("token")

    return if (hasCreds) Screen.MainScreen.route else Screen.LoginScreen.route
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
        enterTransition = { fadeIn(tween(500)) },
        exitTransition  = { fadeOut(tween(500)) }
    ) {
        composable(Screen.MainScreen.route)  { MainScreen() }
        composable(Screen.LoginScreen.route) { LoginScreen(navController) }
    }
}