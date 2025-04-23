package hu.bme.aut.mantella.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import okhttp3.*
import kotlinx.serialization.json.Json
import hu.bme.aut.mantella.model.CollectivesEmojiResponse

class NextcloudRepo(
    private val credsStore: CredentialStore,
    private val client: OkHttpClient
) {
    suspend fun listCollectives(): List<String> = withContext(Dispatchers.IO) {
        val creds = credsStore.get() ?: throw IllegalStateException("Missing credentials")

        val url  = creds.server.trimEnd('/') +
                "/remote.php/dav/files/${creds.username}/Collectives/"

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

    suspend fun getCollectivesEmojis(): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val creds = credsStore.get() ?: error("Missing credentials")
            val baseUrl = creds.server.trimEnd('/')

            val request = Request.Builder()
                .url("$baseUrl/index.php/apps/collectives/_api")
                .header("OCS-APIREQUEST", "true")
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                check(resp.isSuccessful) { "HTTP ${resp.code}: ${resp.message}" }

                val body  = resp.body?.string() ?: error("Empty response")
                val json = Json { ignoreUnknownKeys = true }
                val parsed = json.decodeFromString<CollectivesEmojiResponse>(body)
                parsed.data.map { it.name to it.emoji }

            }
        }
}