package hu.bme.aut.mantella.data

import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.Interceptor

class AuthInterceptor(private val credentialStore: CredentialStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = runBlocking {
        val creds = credentialStore.get()
        val reqBuilder = chain.request().newBuilder()
        creds?.let {
            reqBuilder.header(
                "Authorization",
                Credentials.basic(it.username, it.appPassword)
            )
        }
        reqBuilder.header("OCS-APIRequest", "true")
        chain.proceed(reqBuilder.build())
    }
}