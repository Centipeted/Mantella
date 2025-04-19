package hu.bme.aut.mantella.navigation

sealed class Screen(val route: String) {
    object MainScreen : Screen("main")
    object LoginScreen : Screen("login")
}