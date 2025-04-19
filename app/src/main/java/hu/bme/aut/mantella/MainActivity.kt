package hu.bme.aut.mantella

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.helper.SingleAccountHelper
import hu.bme.aut.mantella.navigation.NavGraph
import hu.bme.aut.mantella.ui.theme.MantellaTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MantellaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        AccountImporter.onActivityResult(requestCode, resultCode, data, this) { account ->
            SingleAccountHelper.commitCurrentAccount(applicationContext, account.name)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        AccountImporter.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}