package hu.bme.aut.mantella.data

import hu.bme.aut.mantella.screens.loginScreen.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import okhttp3.*

class NextcloudRepo(
    private val credsStore: CredentialStore,
    private val client: OkHttpClient
) {
    suspend fun listRoot(): List<String> = withContext(Dispatchers.IO) {
        val creds = credsStore.get() ?: throw IllegalStateException("Missing credentials")

        val url  = creds.server.trimEnd('/') +
                "/remote.php/dav/files/${creds.username}/"

        val body = """
            <?xml version="1.0" encoding="utf-8" ?>
            <d:propfind xmlns:d="DAV:">
              <d:prop><d:displayname/></d:prop>
            </d:propfind>
        """.trimIndent().toRequestBody("text/xml; charset=utf-8".toMediaType())

        val req  = Request.Builder()
            .url(url)
            .method("PROPFIND", body)
            .header("Depth", "1")
            .build()

        client.newCall(req).execute().use { resp ->
            if (resp.code != 207) throw IOException("HTTP ${resp.code}")
            val xml = resp.body?.string() ?: ""
            Regex("<d:displayname>(.*?)</d:displayname>")
                .findAll(xml)
                .map { it.groupValues[1] }
                .drop(1)
                .toList()
        }
    }
}