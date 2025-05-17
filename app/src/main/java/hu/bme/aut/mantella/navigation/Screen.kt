package hu.bme.aut.mantella.navigation

import hu.bme.aut.mantella.model.CollectivePage
import hu.bme.aut.mantella.model.CollectiveWithEmoji
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object MainScreen : Screen("main")
    object LoginScreen : Screen("login")
    object CollectivePagesScreen : Screen("pages/{collectiveName}/{collectiveEmoji}") {
        fun createRoute(collective: CollectiveWithEmoji): String =
            "pages/${collective.name}/${collective.emoji}"
    }
    object MarkdownViewerScreen : Screen("viewer/{pageName}/{pagePath}") {
        fun createRoute(collectivePage: CollectivePage): String {
            val encodedPath = URLEncoder.encode(collectivePage.path, StandardCharsets.UTF_8.toString())
            return "viewer/${collectivePage.name}/$encodedPath"
        }
    }
    object NewCollectiveScreen : Screen("new collective")
}