package hu.bme.aut.mantella.data

import hu.bme.aut.mantella.model.CollectivePage
import hu.bme.aut.mantella.model.CollectiveWithEmoji
import hu.bme.aut.mantella.model.CollectivesEmojiResponse
import hu.bme.aut.mantella.model.GroupsPayload
import hu.bme.aut.mantella.model.OcsRoot
import hu.bme.aut.mantella.model.UsersPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class NextcloudRepo(
    private val credentialsStore: CredentialStore,
    private val client: OkHttpClient
) {
    suspend fun listCollectives(): List<String> = withContext(Dispatchers.IO) {
        val creds = credentialsStore.get() ?: throw IllegalStateException("Missing credentials")

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

    suspend fun fetchCollectivesWithPages(): Map<CollectiveWithEmoji, List<CollectivePage>> =
        withContext(Dispatchers.IO) {
            val creds = credentialsStore.get() ?: error("Missing credentials")
            val base  = creds.server.trimEnd('/')
            val user  = creds.username

            val collectives = getCollectivesEmojis()
                .map { (n, e) -> CollectiveWithEmoji(n, e) }

            supervisorScope {
                collectives.associateWith { collective ->
                    async {
                        val url = "$base/remote.php/dav/files/$user/Collectives/${collective.name}/"

                        val body = """
                        <?xml version="1.0" encoding="utf-8" ?>
                        <d:propfind xmlns:d="DAV:">
                          <d:prop><d:displayname/></d:prop>
                        </d:propfind>
                    """.trimIndent()
                            .toRequestBody("text/xml; charset=utf-8".toMediaType())

                        val req = Request.Builder()
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
                                .filter { it.endsWith(".md") }
                                .map { file ->
                                    CollectivePage(
                                        mainPage = file.equals("Readme.md", ignoreCase = true),
                                        name = file.removeSuffix(".md"),
                                        path = "${collective.name}/$file"
                                    )
                                }
                                .toList()
                        }
                    }
                }.mapValues { it.value.await() }
            }
        }

    suspend fun getMarkdownFile(path: String): String = withContext(Dispatchers.IO) {
        val creds = credentialsStore.get() ?: error("Missing credentials")
        val base  = creds.server.trimEnd('/')
        val user  = creds.username

        val url = "$base/remote.php/dav/files/$user/Collectives/$path"
        println("url: $url")

        val req = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            if (resp.code != 200) {
                throw IOException("HTTP ${resp.code}")
            }
            println("Response: ${resp.code} ${resp.message}")
            resp.body?.string() ?: ""
        }
    }

    suspend fun saveMarkdownFile(path: String, content: String) = withContext(Dispatchers.IO) {
        val creds = credentialsStore.get() ?: error("Missing credentials")
        val base  = creds.server.trimEnd('/')
        val user  = creds.username

        val url = "$base/remote.php/dav/files/$user/Collectives/$path"
        println("Saving to: $url")

        val req = Request.Builder()
            .url(url)
            .put(content.toRequestBody("text/markdown".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("Upload failed: HTTP ${resp.code}")
            }
            println("Saved successfully")
        }
    }

    suspend fun getAllUsersAndGroups(limit: Int = 500): Pair<List<String>, List<String>> =
        withContext(Dispatchers.IO) {
            val creds = credentialsStore.get() ?: error("Missing credentials")
            val base  = creds.server.trimEnd('/')

            val json      = Json { ignoreUnknownKeys = true }
            val ocsHeader = "OCS-APIREQUEST"

            val usersReq  = Request.Builder()
                .url("$base/ocs/v1.php/cloud/users?search=&limit=$limit&offset=0&format=json")
                .header(ocsHeader, "true")
                .get()
                .build()

            val groupsReq = Request.Builder()
                .url("$base/ocs/v1.php/cloud/groups?search=&limit=$limit&offset=0&format=json")
                .header(ocsHeader, "true")
                .get()
                .build()

            supervisorScope {
                val usersDef  = async {
                    client.newCall(usersReq).execute().use { r ->
                        check(r.isSuccessful) { "Users HTTP ${r.code}" }
                        val body = r.body?.string() ?: ""
                        json.decodeFromString<OcsRoot<UsersPayload>>(body).ocs.data.users
                    }
                }
                val groupsDef = async {
                    client.newCall(groupsReq).execute().use { r ->
                        check(r.isSuccessful) { "Groups HTTP ${r.code}" }
                        val body = r.body?.string() ?: ""
                        json.decodeFromString<OcsRoot<GroupsPayload>>(body).ocs.data.groups
                    }
                }
                usersDef.await() to groupsDef.await()
            }
        }

    suspend fun createCollectiveOfficial(
        title:  String,
        emoji:  String,
        users:  List<String>,
        groups: List<String>
    ) = withContext(Dispatchers.IO) {

        val creds = credentialsStore.get() ?: error("Missing credentials")
        val base  = creds.server.trimEnd('/')
        val createBody = FormBody.Builder()
            .add("name",  title.trim())
            .add("emoji", emoji)
            .build()

        val createUrl  = "$base/index.php/apps/collectives/_api"

        val responseStr: String
        val collectiveId: Int

        client.newCall(
            Request.Builder()
                .url(createUrl)
                .header("OCS-APIREQUEST", "true")
                .post(createBody)
                .build()
        ).execute().use { r ->
            responseStr = r.body?.string() ?: ""

            check(r.isSuccessful) { "Collective create failed: HTTP ${r.code}" }
            collectiveId =
                Json.decodeFromString<JsonObject>(responseStr)
                    .jsonObject["id"]!!.jsonPrimitive.int
        }

        shareCollectiveFolder(
            base = base,
            collectiveId = collectiveId,
            users = users.filterNot { it == creds.username },
            groups = groups,
        )
    }


    private suspend fun shareCollectiveFolder(
        base: String,
        collectiveId: Int,
        users: List<String>,
        groups: List<String>
    ) {
        val shareUrl  = "$base/ocs/v2.php/apps/files_sharing/api/v1/shares"
        val ocsHdr    = "OCS-APIREQUEST"
        val permAll   = "31"

        fun share(target: String, type: Int) {
            val body = FormBody.Builder()
                .add("path", "/collectives/$collectiveId")
                .add("shareType", type.toString())
                .add("shareWith", target)
                .add("permissions", permAll)
                .build()

            client.newCall(
                Request.Builder()
                    .url(shareUrl)
                    .header(ocsHdr, "true")
                    .post(body)
                    .build()
            ).execute().use { r ->
                check(r.isSuccessful) { "Share failed: HTTP ${r.code}" }
            }
        }

        val currentUser = credentialsStore.get()?.username ?: ""
        users.filterNot { it == currentUser }
            .forEach { share(it, 0) }
        groups.forEach { share(it, 1) }
    }

    suspend fun deleteMarkdownFiles(paths: List<String>) = withContext(Dispatchers.IO) {
        val creds = credentialsStore.get() ?: error("Missing credentials")
        val base  = creds.server.trimEnd('/')
        val user  = creds.username

        supervisorScope {
            paths.map { relPath ->
                async {
                    val url = "$base/remote.php/dav/files/$user/Collectives/$relPath"
                    val req = Request.Builder().url(url).delete().build()

                    client.newCall(req).execute().use { resp ->
                        if (resp.code !in listOf(200, 204, 404)) {
                            throw IOException("Delete $relPath failed: HTTP ${resp.code}")
                        }
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun getCollectivesEmojis(): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val creds = credentialsStore.get() ?: error("Missing credentials")
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