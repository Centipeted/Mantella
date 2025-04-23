package hu.bme.aut.mantella.data

import hu.bme.aut.mantella.model.AuthCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object NextcloudApi {
    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("OCS-APIRequest", "true")
                    .build()
            )
        }
        .build()

    suspend fun generateAppPassword(
        server: String,
        username: String,
        password: String
    ): String = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val url = server.trimEnd('/') + "/ocs/v2.php/core/getapppassword"

        val req: Request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(username, password))
            .header("User-Agent", "NextcloudAndroidSample/1.0")
            .build()

        client.newCall(req).execute().use { resp ->
            when (resp.code) {
                200 -> {
                    val xml = resp.body?.string()
                        ?: throw IOException("Empty body from server")
                    Regex("<apppassword>(.*)</apppassword>")
                        .find(xml)?.groupValues?.get(1)
                        ?: throw IOException("Token not present in response")
                }
                403 -> password // we were already using an app‑password
                else -> throw IOException("HTTP ${resp.code}: ${resp.message}")
            }
        }
    }

    suspend fun revokeAppPassword(creds: AuthCredentials) =
        withContext(Dispatchers.IO) {
            val url = creds.server.trimEnd('/') + "/ocs/v2.php/core/apppassword"

            val req = Request.Builder()
                .url(url)
                .delete()
                .header("Authorization", Credentials.basic(creds.username, creds.appPassword))
                .header("User-Agent", "NextcloudAndroidSample/1.0")
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.code != 200) {
                    println("App-password revoke failed: HTTP ${resp.code}")
                }
            }
        }
}